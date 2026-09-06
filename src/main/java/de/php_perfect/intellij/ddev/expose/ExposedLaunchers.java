package de.php_perfect.intellij.ddev.expose;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import de.php_perfect.intellij.ddev.version.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Prepares the launcher directory of a project's exposed commands.
 */
public final class ExposedLaunchers {
    private static final Logger LOG = Logger.getInstance(ExposedLaunchers.class);
    // DDEV 1.24.7 made `ddev exec` return the command's exit code and added --quiet.
    private static final @NotNull Version QUIET_EXEC_VERSION = new Version("1.24.7");
    private static final int CHMOD_TIMEOUT = 15_000;

    private ExposedLaunchers() {
    }

    /**
     * Writes the launchers and returns their directory as a local path to put on the terminal's
     * PATH, or {@code null} when nothing is exposed. For WSL projects the directory lies inside the
     * distribution, and the terminal translates it to the distribution's path.
     */
    public static @Nullable String prepare(@NotNull Project project) {
        final List<ExposedCommands.Launcher> launchers = ExposedCommands.launchers(project);
        final State state = DdevStateManager.getInstance(project).getState();
        final String projectRoot = DdevProjectRoot.of(project);

        if (launchers.isEmpty() || !state.isConfigured() || state.getDdevBinary() == null || projectRoot == null) {
            return null;
        }

        final Description description = state.getDescription();
        final String projectName = description == null || description.getName() == null ? "" : description.getName();
        final Version version = state.getDdevVersion();
        final boolean quietFlag = version != null && version.compareTo(QUIET_EXEC_VERSION) >= 0;

        try {
            final WslPath wslProject = SystemInfo.isWindows ? WslPath.parseWindowsUncPath(projectRoot) : null;
            if (wslProject != null) {
                final String directory = wslProject.getWslRoot() + "\\tmp\\ddev-intellij-plugin\\exposed\\" + project.getLocationHash();
                final List<Path> written = LauncherWriter.write(Path.of(directory), LauncherWriter.Target.WSL, projectName,
                        wslProject.getLinuxPath(), state.getDdevBinary(), launchers, quietFlag);
                makeExecutableInWsl(projectRoot, written);
                return directory;
            }

            final Path directory = Path.of(PathManager.getSystemPath(), "ddev-integration", "exposed", project.getLocationHash());
            LauncherWriter.write(directory, SystemInfo.isWindows ? LauncherWriter.Target.WINDOWS : LauncherWriter.Target.UNIX,
                    projectName, projectRoot, state.getDdevBinary(), launchers, quietFlag);
            return directory.toString();
        } catch (IOException exception) {
            LOG.info("Could not write the exposed DDEV commands", exception);
            return null;
        }
    }

    /**
     * Files written through the \\wsl$ share do not carry the executable bit, so it is set inside the distribution.
     */
    private static void makeExecutableInWsl(@NotNull String projectRoot, @NotNull List<Path> launchers) {
        if (launchers.isEmpty()) {
            return;
        }

        final List<String> command = new ArrayList<>(List.of("chmod", "755"));
        for (Path launcher : launchers) {
            final WslPath wslPath = WslPath.parseWindowsUncPath(launcher.toString());
            if (wslPath != null) {
                command.add(wslPath.getLinuxPath());
            }
        }

        // The executor runs the command inside the distribution of the WSL working directory.
        final GeneralCommandLine commandLine = new GeneralCommandLine(command).withWorkDirectory(projectRoot);
        try {
            final ProcessOutput output = ProcessExecutor.getInstance().executeCommandLine(commandLine, CHMOD_TIMEOUT, false);
            if (output.getExitCode() != 0) {
                LOG.info("Could not make the exposed DDEV commands executable: " + output.getStderr());
            }
        } catch (ExecutionException exception) {
            LOG.info("Could not make the exposed DDEV commands executable", exception);
        }
    }
}
