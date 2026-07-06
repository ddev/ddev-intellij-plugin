package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.reflect.TypeToken;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.parser.JsonParser;
import de.php_perfect.intellij.ddev.cmd.parser.JsonParserException;
import de.php_perfect.intellij.ddev.version.Version;
import org.apache.commons.compress.utils.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DdevImpl implements Ddev {
    // Short timeout for simple version command that should be nearly instant
    private static final int VERSION_COMMAND_TIMEOUT = 15_000;

    // Medium timeout for detailed version command with JSON parsing
    private static final int DETAILED_VERSION_COMMAND_TIMEOUT = 30_000;

    // Long timeout for status commands due to possibly being blocked by ddev being busy
    private static final int STATUS_COMMAND_TIMEOUT = 300_000;

    // Medium timeout for add-on listing which may fetch the registry from the network
    private static final int ADD_ON_LIST_COMMAND_TIMEOUT = 60_000;

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

    @Override
    public @NotNull Description describeProject(final @NotNull String binary, final @NotNull Project project, final @NotNull String projectName) throws CommandFailedException {
        return execute(binary, List.of("describe", projectName), Description.class, project, STATUS_COMMAND_TIMEOUT);
    }

    @Override
    public @NotNull List<AddOn> listAddOns(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        final Type type = TypeToken.getParameterized(List.class, AddOn.class).getType();
        return execute(binary, List.of("add-on", "list", "--all"), type, project, ADD_ON_LIST_COMMAND_TIMEOUT);
    }

    @Override
    public @NotNull List<InstalledAddOn> listInstalledAddOns(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        final Type type = TypeToken.getParameterized(List.class, InstalledAddOn.class).getType();
        return execute(binary, List.of("add-on", "list", "--installed"), type, project, ADD_ON_LIST_COMMAND_TIMEOUT);
    }

    @Override
    public @NotNull List<DdevProject> listProjects(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        final Type type = TypeToken.getParameterized(List.class, DdevProject.class).getType();
        return execute(binary, List.of("list"), type, project, STATUS_COMMAND_TIMEOUT);
    }

    @Override
    public @NotNull List<Snapshot> listSnapshots(final @NotNull String binary, final @NotNull Project project) throws CommandFailedException {
        return this.listSnapshots(binary, project, null);
    }

    @Override
    public @NotNull List<Snapshot> listSnapshots(final @NotNull String binary, final @NotNull Project project, final @Nullable String workingDirectory) throws CommandFailedException {
        // The raw payload maps each project name to its snapshot list (null when there are none).
        final Type listType = TypeToken.getParameterized(List.class, Snapshot.class).getType();
        final Type type = TypeToken.getParameterized(Map.class, String.class, listType).getType();
        final Map<String, List<Snapshot>> snapshotsByProject = execute(binary, List.of("snapshot", "--list"), type, project, STATUS_COMMAND_TIMEOUT, workingDirectory);

        return snapshotsByProject.values().stream()
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .toList();
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
        return execute(binary, List.of(action), type, project, timeout);
    }

    private @NotNull <T> T execute(final @NotNull String binary, final @NotNull List<String> actionArguments, final @NotNull Type type, final @NotNull Project project, int timeout) throws CommandFailedException {
        return execute(binary, actionArguments, type, project, timeout, null);
    }

    private @NotNull <T> T execute(final @NotNull String binary, final @NotNull List<String> actionArguments, final @NotNull Type type, final @NotNull Project project, int timeout, final @Nullable String workingDirectory) throws CommandFailedException {
        final GeneralCommandLine commandLine = createDdevCommandLine(binary, actionArguments, project, true, workingDirectory);

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

    private @NotNull GeneralCommandLine createDdevCommandLine(final @NotNull String binary, final @NotNull String action, final @NotNull Project project, boolean json) {
        return this.createDdevCommandLine(binary, List.of(action), project, json, null);
    }

    private @NotNull GeneralCommandLine createDdevCommandLine(final @NotNull String binary, final @NotNull List<String> actionArguments, final @NotNull Project project, boolean json, final @Nullable String workingDirectory) {
        final ArrayList<String> arguments = Lists.newArrayList();
        arguments.add(binary);
        arguments.addAll(actionArguments);

        if (json) {
            arguments.add("--json-output");
        }

        return new GeneralCommandLine(arguments)
                .withWorkDirectory(workingDirectory != null ? workingDirectory : project.getBasePath())
                .withEnvironment("DDEV_NONINTERACTIVE", "true");
    }
}
