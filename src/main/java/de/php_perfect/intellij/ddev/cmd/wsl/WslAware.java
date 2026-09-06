package de.php_perfect.intellij.ddev.cmd.wsl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.wsl.WSLCommandLineOptions;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

public class WslAware {
    private WslAware() {
    }

    public static <T extends GeneralCommandLine> T patchCommandLine(T commandLine) {
        return patchCommandLine(commandLine, false);
    }

    public static <T extends GeneralCommandLine> T patchCommandLine(T commandLine, boolean loginShell) {
        if (commandLine.getWorkDirectory() == null) {
            return commandLine;
        }
        WSLDistribution distribution = WslPath.getDistributionByWindowsUncPath(commandLine.getWorkDirectory().getPath());

        if (distribution == null) {
            return commandLine;
        }

        try {
            return applyWslPatch(commandLine, distribution, loginShell);
        } catch (ExecutionException ignored) {
            return commandLine;
        }
    }

    public static @Nullable String toHostPath(@Nullable String path, @Nullable String projectDirectory) {
        if (path == null || !path.startsWith("/") || path.startsWith("//") || projectDirectory == null) {
            return path;
        }
        final WslPath projectPath = WslPath.parseWindowsUncPath(projectDirectory);
        if (projectPath == null) {
            return path;
        }
        if (projectPath.getLinuxPath().equals(path)) {
            return projectDirectory;
        }
        // Keep the distro even for /mnt/c paths, which getWindowsPath converts to a host drive.
        return projectPath.getWslRoot() + path.replace('/', '\\');
    }

    public static @NotNull String toCommandPath(@NotNull String path, @Nullable String workingDirectory) {
        final WSLDistribution distribution = workingDirectory == null ? null
                : WslPath.getDistributionByWindowsUncPath(workingDirectory);
        if (distribution == null) {
            return path;
        }
        final String wslPath = distribution.getWslPath(Path.of(path));
        return wslPath == null ? path : wslPath;
    }

    @NotNull
    private static <T extends GeneralCommandLine> T applyWslPatch(T generalCommandLine, WSLDistribution distribution, boolean loginShell) throws ExecutionException {
        WSLCommandLineOptions options = new WSLCommandLineOptions()
                .setExecuteCommandInLoginShell(loginShell);

        return distribution.patchCommandLine(generalCommandLine, null, options);
    }
}
