package de.php_perfect.intellij.ddev.cms;

import com.intellij.execution.configurations.PtyCommandLine;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Runner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

public final class CmsProjectInstaller {
    private CmsProjectInstaller() {
    }

    public static void install(@NotNull Project project, @NotNull String workingDirectory,
                               @NotNull String projectName, @NotNull CmsInstallationRecipe recipe,
                               @NotNull Credentials credentials, @Nullable Runnable afterCompletion) {
        runStep(project, workingDirectory, projectName, recipe, credentials, 0, null, afterCompletion);
    }

    private static void runStep(@NotNull Project project, @NotNull String workingDirectory,
                                @NotNull String projectName, @NotNull CmsInstallationRecipe recipe,
                                @NotNull Credentials credentials, int index, @Nullable String primaryUrl,
                                @Nullable Runnable afterCompletion) {
        if (index >= recipe.steps().size()) {
            if (afterCompletion != null) {
                afterCompletion.run();
            }
            return;
        }

        final CmsInstallationRecipe.Step step = recipe.steps().get(index);
        final CmsInstallationRecipe.Context context = new CmsInstallationRecipe.Context(
                projectName, primaryUrl != null ? primaryUrl : "", credentials.username(),
                credentials.password(), credentials.email(), credentials.loginToken());
        final List<String> arguments = recipe.resolveArguments(step, context);
        final List<CmsInstallationRecipe.FileAction> fileActions = recipe.resolveFileActions(step, context);

        if (arguments.isEmpty()) {
            applyFileActions(project, workingDirectory, projectName, recipe, credentials, index, primaryUrl,
                    afterCompletion, fileActions);
            return;
        }

        final String binary = Objects.requireNonNull(
                DdevStateManager.getInstance(project).getState().getDdevBinary());
        final GeneralCommandLine commandLine = step.sensitive() ? new GeneralCommandLine() : new PtyCommandLine();
        commandLine.setExePath(binary);
        final byte[] standardInput;
        if (step.sensitive()) {
            commandLine.addParameters("exec", "bash", "-s");
            standardInput = ("set -euo pipefail\nexec " + arguments.stream()
                    .map(CmsProjectInstaller::shellQuote)
                    .collect(java.util.stream.Collectors.joining(" ")) + "\n")
                    .getBytes(StandardCharsets.UTF_8);
        } else {
            commandLine.addParameters(arguments);
            standardInput = null;
        }
        commandLine.setWorkDirectory(workingDirectory);
        commandLine.setCharset(StandardCharsets.UTF_8);
        commandLine.withEnvironment("DDEV_NONINTERACTIVE", "true");

        Runner.getInstance(project).runWithOutcome(commandLine, step.title(), standardInput, () -> {
            if (fileActions.isEmpty()) {
                continueAfterStep(project, workingDirectory, projectName, recipe, credentials, index, primaryUrl,
                        afterCompletion);
            } else {
                applyFileActions(project, workingDirectory, projectName, recipe, credentials, index, primaryUrl,
                        afterCompletion, fileActions);
            }
        }, () -> showStepFailure(project, step, () -> runStep(project, workingDirectory, projectName, recipe,
                credentials, index, primaryUrl, afterCompletion)));
    }

    static @NotNull String shellQuote(@NotNull String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static void applyFileActions(@NotNull Project project, @NotNull String workingDirectory,
                                         @NotNull String projectName, @NotNull CmsInstallationRecipe recipe,
                                         @NotNull Credentials credentials, int index, @Nullable String primaryUrl,
                                         @Nullable Runnable afterCompletion,
                                         @NotNull List<CmsInstallationRecipe.FileAction> fileActions) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path projectRoot = Path.of(workingDirectory).toAbsolutePath().normalize();
                for (CmsInstallationRecipe.FileAction fileAction : fileActions) {
                    switch (fileAction) {
                        case CmsInstallationRecipe.WriteFile writeFile ->
                                writeProjectFile(projectRoot, writeFile);
                        case CmsInstallationRecipe.MoveContents moveContents ->
                                moveProjectContents(projectRoot, moveContents.relativeDirectory());
                    }
                }
                ApplicationManager.getApplication().invokeLater(() -> continueAfterStep(project,
                        workingDirectory, projectName, recipe, credentials, index, primaryUrl, afterCompletion));
            } catch (Exception exception) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                        DdevIntegrationBundle.message("cms.install.files.failed", exception.getMessage()) + "\n\n"
                                + DdevIntegrationBundle.message("cms.install.partialState"),
                        DdevIntegrationBundle.message("cms.install.failed.title")));
            }
        });
    }

    private static void showStepFailure(@NotNull Project project, @NotNull CmsInstallationRecipe.Step step,
                                        @NotNull Runnable retry) {
        ApplicationManager.getApplication().invokeLater(() -> {
            String message = DdevIntegrationBundle.message("cms.install.step.failed", step.title());
            if (step.failureHelp() != null && !step.failureHelp().isBlank()) {
                message += "\n\n" + step.failureHelp();
            }
            message += "\n\n" + DdevIntegrationBundle.message("cms.install.partialState");
            final int choice = Messages.showDialog(project, message,
                    DdevIntegrationBundle.message("cms.install.failed.title"),
                    new String[]{DdevIntegrationBundle.message("cms.install.retry"),
                            DdevIntegrationBundle.message("cms.install.stop")},
                    0, Messages.getErrorIcon());
            if (choice == 0) {
                retry.run();
            }
        });
    }

    static void writeProjectFile(@NotNull Path projectRoot,
                                 @NotNull CmsInstallationRecipe.WriteFile writeFile)
            throws java.io.IOException {
        final Path target = resolveWithinProject(projectRoot, writeFile.relativePath());
        Files.createDirectories(target.getParent());
        Files.writeString(target, writeFile.content(), StandardCharsets.UTF_8);
    }

    static void moveProjectContents(@NotNull Path projectRoot, @NotNull String relativeDirectory)
            throws java.io.IOException {
        final Path source = resolveWithinProject(projectRoot, relativeDirectory);
        if (!Files.isDirectory(source)) {
            throw new java.io.IOException("Generated project directory does not exist: " + relativeDirectory);
        }

        try (var entries = Files.list(source)) {
            for (Path entry : entries.toList()) {
                final Path target = projectRoot.resolve(entry.getFileName()).normalize();
                if (Files.exists(target)) {
                    throw new java.io.IOException("Generated project conflicts with existing file: "
                            + entry.getFileName());
                }
                Files.move(entry, target);
            }
        }
        Files.delete(source);
    }

    static @NotNull Path resolveWithinProject(@NotNull Path projectRoot, @NotNull String relativePath) {
        final Path resolved = projectRoot.resolve(relativePath).normalize();
        if (!resolved.startsWith(projectRoot)) {
            throw new IllegalArgumentException("CMS installation path escapes the project root");
        }
        return resolved;
    }

    private static void continueAfterStep(@NotNull Project project, @NotNull String workingDirectory,
                                          @NotNull String projectName, @NotNull CmsInstallationRecipe recipe,
                                          @NotNull Credentials credentials, int index,
                                          @Nullable String primaryUrl, @Nullable Runnable afterCompletion) {
        if (index == 0) {
            final String binary = Objects.requireNonNull(
                    DdevStateManager.getInstance(project).getState().getDdevBinary());
            resolvePrimaryUrl(project, binary, projectName, resolvedUrl -> runStep(project, workingDirectory,
                    projectName, recipe, credentials, index + 1, resolvedUrl, afterCompletion));
        } else {
            runStep(project, workingDirectory, projectName, recipe, credentials, index + 1,
                    primaryUrl, afterCompletion);
        }
    }

    private static void resolvePrimaryUrl(@NotNull Project project, @NotNull String binary,
                                          @NotNull String projectName,
                                          @NotNull java.util.function.Consumer<String> callback) {
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Description description = Ddev.getInstance().describeProject(binary, project, projectName);
                final String primaryUrl = description.getPrimaryUrl();

                if (primaryUrl == null || primaryUrl.isBlank()) {
                    throw new CommandFailedException("DDEV did not report a primary URL");
                }

                ApplicationManager.getApplication().invokeLater(() -> callback.accept(primaryUrl));
            } catch (CommandFailedException exception) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(project,
                        DdevIntegrationBundle.message("cms.install.primaryUrl.failed"),
                        DdevIntegrationBundle.message("cms.install.failed.title")));
            }
        });
    }

    public record Credentials(@NotNull String username, @NotNull String password, @NotNull String email,
                              @NotNull String loginToken) {
        private static final SecureRandom SECURE_RANDOM = new SecureRandom();
        public static final Credentials NONE = new Credentials("", "", "", "");

        public static @NotNull Credentials create(@NotNull String username, @NotNull String password,
                                                   @NotNull String email) {
            final byte[] token = new byte[32];
            SECURE_RANDOM.nextBytes(token);
            return new Credentials(username, password, email, HexFormat.of().formatHex(token));
        }
    }
}
