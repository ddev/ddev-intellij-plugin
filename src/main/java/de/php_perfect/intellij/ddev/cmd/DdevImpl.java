package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.parser.JsonParser;
import de.php_perfect.intellij.ddev.cmd.parser.JsonParserException;
import de.php_perfect.intellij.ddev.version.Version;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DdevImpl implements Ddev {
    // Short timeout for simple version command that should be nearly instant
    private static final int VERSION_COMMAND_TIMEOUT = 15_000;

    // Medium timeout for detailed version command with JSON parsing
    private static final int DETAILED_VERSION_COMMAND_TIMEOUT = 30_000;

    // Long timeout for status commands due to possibly being blocked by ddev being busy
    private static final int STATUS_COMMAND_TIMEOUT = 300_000;

    @Override
    public @NotNull Version version(@NotNull String binary, @NotNull Project project) throws CommandFailedException {
        final String versionString = this.executeVersionCommand(binary, project);
        final Pattern r = Pattern.compile("ddev version (v.*)$");
        final Matcher m = r.matcher(versionString);

        if (m.find()) {
            return new Version(m.group(1));
        }

        throw new CommandFailedException("Unexpected output of ddev version command: " + versionString);
    }

    public @NotNull Versions detailedVersions(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        return execute(binary, "version", Versions.class, project, DETAILED_VERSION_COMMAND_TIMEOUT);
    }

    public @NotNull Description describe(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        return execute(binary, "describe", Description.class, project, STATUS_COMMAND_TIMEOUT);
    }

    private @NotNull String executeVersionCommand(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        final GeneralCommandLine commandLine = createDdevCommandLine(binary, "--version", project, false);

        try {
            final ProcessOutput processOutput = ProcessExecutor.getInstance().executeCommandLine(commandLine, VERSION_COMMAND_TIMEOUT, false);

            if (processOutput.isTimeout()) {
                throw new CommandFailedException("Command timed out after " + (VERSION_COMMAND_TIMEOUT / 1000) + " seconds: " + commandLine.getCommandLineString() + " in " + commandLine.getWorkDirectory().getPath());
            }

            if (processOutput.getExitCode() != 0) {
                throw new CommandFailedException("Command '" + commandLine.getCommandLineString() + "' returned non zero exit code " + processOutput);
            }

            return processOutput.getStdout();
        } catch (ExecutionException exception) {
            throw new CommandFailedException("Failed to execute " + commandLine.getCommandLineString(), exception);
        }
    }

    private @NotNull <T> T execute(final @NotNull String binary, final @NotNull String action, final @NotNull Type type, final @NotNull Project project, int timeout) throws CommandFailedException {
        final GeneralCommandLine commandLine = createDdevCommandLine(binary, action, project);

        ProcessOutput processOutput = null;
        try {
            processOutput = ProcessExecutor.getInstance().executeCommandLine(commandLine, timeout, false);

            if (processOutput.isTimeout()) {
                throw new CommandFailedException("Command timed out after " + (timeout / 1000) + " seconds: " + commandLine.getCommandLineString() + " in " + commandLine.getWorkDirectory().getPath());
            }

            if (processOutput.getExitCode() != 0) {
                throw new CommandFailedException("Command '" + commandLine.getCommandLineString() + "' returned non zero exit code " + processOutput);
            }

            return JsonParser.getInstance().parse(processOutput.getStdout(), type);
        } catch (ExecutionException exception) {
            throw new CommandFailedException("Failed to execute " + commandLine.getCommandLineString(), exception);
        } catch (JsonParserException exception) {
            throw new CommandFailedException("Failed to parse output of command '" + commandLine.getCommandLineString() + "': " + processOutput.getStdout(), exception);
        }
    }

    private @NotNull GeneralCommandLine createDdevCommandLine(final @NotNull String binary, final @NotNull String action, final @NotNull Project project) {
        return this.createDdevCommandLine(binary, action, project, true);
    }

    private @NotNull GeneralCommandLine createDdevCommandLine(final @NotNull String binary, final @NotNull String action, final @NotNull Project project, boolean json) {
        final ArrayList<String> arguments = Lists.newArrayList();
        arguments.add(binary);
        arguments.add(action);

        if (json) {
            arguments.add("--json-output");
        }

        return new GeneralCommandLine(arguments)
                .withWorkDirectory(project.getBasePath())
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
    }
}
