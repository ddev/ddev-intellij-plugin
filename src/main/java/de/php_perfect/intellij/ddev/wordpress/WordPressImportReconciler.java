package de.php_perfect.intellij.ddev.wordpress;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.CommandLineRedactor;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.Runner;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public final class WordPressImportReconciler {
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");
    private static final int COMMAND_TIMEOUT = 300_000;
    // Read evaluated configuration (including wp-config-ddev.php) before WordPress needs its tables.
    private static final String PREFIX_COMMAND = "WP_CLI::add_command('ddev-integration-prefix', "
            + "static function () { WP_CLI::line($GLOBALS['table_prefix']); }, "
            + "['when' => 'after_wp_config_load']);";

    private WordPressImportReconciler() {
    }

    public static void reconcile(@NotNull Project project, @NotNull String workingDirectory,
                                 @NotNull String projectName) {
        final String binary = Objects.requireNonNull(
                DdevStateManager.getInstance(project).getState().getDdevBinary());

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final List<String> tables = executeLines(project, binary, workingDirectory,
                        List.of("mysql", "-Nse", "SHOW TABLES;"));
                final String targetPrefix = configuredPrefix(project, binary, workingDirectory);
                final String prefix = tables.contains(targetPrefix + "options") && tables.contains(targetPrefix + "posts")
                        ? targetPrefix : detectPrefix(tables);
                if (prefix == null) {
                    return;
                }

                final String siteUrl = execute(project, binary, workingDirectory, List.of("mysql", "-Nse",
                        "SELECT option_value FROM `" + prefix + "options` WHERE option_name='siteurl' LIMIT 1;"));
                final Description description = Ddev.getInstance().describeProject(binary, project, projectName);
                final String targetUrl = description.getPrimaryUrl();
                if (targetUrl == null || targetUrl.isBlank()) {
                    return;
                }

                ApplicationManager.getApplication().invokeLater(() -> reconcileOnEdt(project, binary,
                        workingDirectory, tables, prefix, targetPrefix, siteUrl.trim(), targetUrl));
            } catch (CommandFailedException | ExecutionException exception) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                        DdevIntegrationBundle.message("wordpress.import.inspect.failed"),
                        DdevIntegrationBundle.message("wordpress.import.failed.title")));
            }
        });
    }

    static void reconcileOnEdt(@NotNull Project project, @NotNull String binary,
                                       @NotNull String workingDirectory, @NotNull List<String> tables,
                                       @NotNull String prefix, @NotNull String targetPrefix, @NotNull String siteUrl,
                                       @NotNull String targetUrl) {
        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        final Runnable reconcileUrl = () -> reconcileUrl(project, binary, workingDirectory, siteUrl, targetUrl);

        if (!targetPrefix.equals(prefix)) {
            if (shouldApply(project, WordPressImportPolicy.fromValue(settings.wordpressTablePrefixImportPolicy),
                    DdevIntegrationBundle.message("wordpress.import.prefix.message", prefix, targetPrefix), true)) {
                final String sql = buildPrefixRenameSql(tables, prefix, targetPrefix);
                if (!sql.isBlank()) {
                    runOnSuccess(project, binary, workingDirectory, "Update WordPress table prefix",
                            List.of("mysql", "-e", sql), reconcileUrl);
                }
            }

            // WP-CLI follows wp-config.php's prefix. It cannot safely update URLs until the imported
            // tables have been reconciled with that configured prefix.
            return;
        }

        reconcileUrl.run();
    }

    static @NotNull String configuredPrefix(@NotNull Project project, @NotNull String binary,
                                             @NotNull String workingDirectory)
            throws ExecutionException, CommandFailedException {
        final String prefix = execute(project, binary, workingDirectory,
                List.of("wp", "--exec=" + PREFIX_COMMAND, "ddev-integration-prefix")).trim();
        if (!SAFE_IDENTIFIER.matcher(prefix).matches()) {
            throw new CommandFailedException("Cannot determine the configured WordPress table prefix");
        }
        return prefix;
    }

    private static void reconcileUrl(@NotNull Project project, @NotNull String binary,
                                     @NotNull String workingDirectory, @NotNull String siteUrl,
                                     @NotNull String targetUrl) {
        final Runnable flushRewrites = () -> runOnSuccess(project, binary, workingDirectory,
                "Flush WordPress rewrite rules", List.of("wp", "rewrite", "flush"), null);
        if (siteUrl.isBlank() || normalizeUrl(siteUrl).equals(normalizeUrl(targetUrl))) {
            flushRewrites.run();
            return;
        }

        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        if (!shouldApply(project, WordPressImportPolicy.fromValue(settings.wordpressUrlImportPolicy),
                DdevIntegrationBundle.message("wordpress.import.url.message", siteUrl, targetUrl), false)) {
            flushRewrites.run();
            return;
        }

        runOnSuccess(project, binary, workingDirectory, "Update WordPress URLs",
                buildSearchReplaceArguments(siteUrl, targetUrl), flushRewrites);
    }

    private static boolean shouldApply(@NotNull Project project, @NotNull WordPressImportPolicy policy,
                                       @NotNull String message, boolean prefixPolicy) {
        if (policy == WordPressImportPolicy.ALWAYS) {
            return true;
        }
        if (policy == WordPressImportPolicy.NEVER) {
            return false;
        }

        final String[] options = {
                DdevIntegrationBundle.message("wordpress.import.option.no"),
                DdevIntegrationBundle.message("wordpress.import.option.yes"),
                DdevIntegrationBundle.message("wordpress.import.option.always"),
                DdevIntegrationBundle.message("wordpress.import.option.never")
        };
        final int choice = Messages.showDialog(project, message,
                DdevIntegrationBundle.message("wordpress.import.title"), options, 1, Messages.getQuestionIcon());
        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        if (choice == 2) {
            if (prefixPolicy) settings.wordpressTablePrefixImportPolicy = WordPressImportPolicy.ALWAYS.value();
            else settings.wordpressUrlImportPolicy = WordPressImportPolicy.ALWAYS.value();
        } else if (choice == 3) {
            if (prefixPolicy) settings.wordpressTablePrefixImportPolicy = WordPressImportPolicy.NEVER.value();
            else settings.wordpressUrlImportPolicy = WordPressImportPolicy.NEVER.value();
        }

        return choice == 1 || choice == 2;
    }

    static String detectPrefix(@NotNull List<String> tables) {
        for (String table : tables) {
            final String trimmed = table.trim();
            if (trimmed.endsWith("options")) {
                final String prefix = trimmed.substring(0, trimmed.length() - "options".length());
                if (SAFE_IDENTIFIER.matcher(prefix).matches()
                        && tables.stream().map(String::trim).anyMatch((prefix + "posts")::equals)) {
                    return prefix;
                }
            }
        }
        return null;
    }

    static @NotNull String buildPrefixRenameSql(@NotNull List<String> tables, @NotNull String from,
                                                 @NotNull String to) {
        if (!SAFE_IDENTIFIER.matcher(from).matches() || !SAFE_IDENTIFIER.matcher(to).matches()) {
            throw new IllegalArgumentException("Unsafe WordPress table prefix");
        }

        final ArrayList<String> statements = new ArrayList<>();
        for (String table : tables.stream().map(String::trim).sorted().toList()) {
            if (table.startsWith(from) && SAFE_IDENTIFIER.matcher(table).matches()) {
                statements.add("RENAME TABLE `" + table + "` TO `" + to + table.substring(from.length()) + "`");
            }
        }

        statements.add("UPDATE `" + to + "options` SET option_name=CONCAT('" + to
                + "', SUBSTRING(option_name," + (from.length() + 1) + ")) WHERE LEFT(option_name,"
                + from.length() + ")='" + from + "'");
        statements.add("UPDATE `" + to + "usermeta` SET meta_key=CONCAT('" + to
                + "', SUBSTRING(meta_key," + (from.length() + 1) + ")) WHERE LEFT(meta_key,"
                + from.length() + ")='" + from + "'");
        return String.join(";", statements) + ";";
    }

    private static @NotNull String normalizeUrl(@NotNull String url) {
        return url.replaceFirst("^https?://", "").replaceFirst("/+$", "");
    }

    static @NotNull List<String> buildSearchReplaceArguments(@NotNull String siteUrl,
                                                              @NotNull String targetUrl) {
        return List.of("wp", "search-replace", normalizeUrl(siteUrl), normalizeUrl(targetUrl),
                "--skip-columns=guid", "--all-tables");
    }

    private static void runOnSuccess(@NotNull Project project, @NotNull String binary,
                                     @NotNull String workingDirectory, @NotNull String title,
                                     @NotNull List<String> arguments, Runnable afterCompletion) {
        final PtyCommandLine commandLine = new PtyCommandLine();
        commandLine.setExePath(binary);
        commandLine.addParameters(arguments);
        commandLine.setWorkDirectory(workingDirectory);
        commandLine.setCharset(StandardCharsets.UTF_8);
        commandLine.withEnvironment("DDEV_NONINTERACTIVE", "true");
        Runner.getInstance(project).runOnSuccess(commandLine, title, afterCompletion);
    }

    private static @NotNull List<String> executeLines(@NotNull Project project, @NotNull String binary,
                                                       @NotNull String workingDirectory,
                                                       @NotNull List<String> arguments)
            throws ExecutionException, CommandFailedException {
        final String output = execute(project, binary, workingDirectory, arguments);
        return output.lines().filter(line -> !line.isBlank()).toList();
    }

    private static @NotNull String execute(@NotNull Project project, @NotNull String binary,
                                           @NotNull String workingDirectory, @NotNull List<String> arguments)
            throws ExecutionException, CommandFailedException {
        final ArrayList<String> command = new ArrayList<>();
        command.add(binary);
        command.addAll(arguments);
        final GeneralCommandLine commandLine = new GeneralCommandLine(command)
                .withWorkDirectory(workingDirectory)
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
        final ProcessOutput output = ProcessExecutor.getInstance().executeCommandLine(
                commandLine, COMMAND_TIMEOUT, false);
        if (output.isTimeout() || output.getExitCode() != 0) {
            throw new CommandFailedException("Command failed: " + CommandLineRedactor.describe(commandLine));
        }
        return output.getStdout();
    }
}
