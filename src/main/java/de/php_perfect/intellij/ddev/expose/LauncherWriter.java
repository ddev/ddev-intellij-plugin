package de.php_perfect.intellij.ddev.expose;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps a directory holding exactly the launchers of the exposed commands.
 */
final class LauncherWriter {
    enum Target {
        /**
         * macOS and Linux: bash launchers.
         */
        UNIX,
        /**
         * Native Windows: {@code .cmd} files for cmd and PowerShell, plus bash launchers for Git Bash.
         */
        WINDOWS,
        /**
         * A project inside a WSL distribution: bash launchers stored in the distribution.
         */
        WSL
    }

    /**
     * @param approot    the project root as the launchers' shell sees it
     * @param ddevBinary the DDEV binary as the launchers' shell sees it
     * @return the bash launchers that were (re)written and may still need to be made executable
     */
    static @NotNull List<Path> write(@NotNull Path directory, @NotNull Target target, @NotNull String projectName,
                                     @NotNull String approot, @NotNull String ddevBinary,
                                     @NotNull List<ExposedCommands.Launcher> launchers, boolean quietFlag)
            throws IOException {
        final Map<String, String> files = new LinkedHashMap<>();
        final List<String> bashLaunchers = new ArrayList<>();

        for (ExposedCommands.Launcher launcher : launchers) {
            final String bashApproot = target == Target.WINDOWS ? approot.replace('\\', '/') : approot;
            final String bashBinary = target == Target.WINDOWS ? ddevBinary.replace('\\', '/') : ddevBinary;
            files.put(launcher.name(), LauncherScripts.bash(projectName, bashApproot, bashBinary, launcher.command(), quietFlag));
            bashLaunchers.add(launcher.name());
            if (target == Target.WINDOWS) {
                files.put(launcher.name() + ".cmd", LauncherScripts.cmd(launcher.command()));
            }
        }
        if (target == Target.WINDOWS && !launchers.isEmpty()) {
            files.put(LauncherScripts.POWERSHELL_LAUNCHER, LauncherScripts.powershell(projectName, approot, ddevBinary, quietFlag));
        }

        Files.createDirectories(directory);
        try (DirectoryStream<Path> existing = Files.newDirectoryStream(directory)) {
            for (Path file : existing) {
                if (!files.containsKey(file.getFileName().toString())) {
                    Files.deleteIfExists(file);
                }
            }
        }

        final List<Path> written = new ArrayList<>();
        for (Map.Entry<String, String> file : files.entrySet()) {
            final Path path = directory.resolve(file.getKey());
            if (!Files.isRegularFile(path) || !file.getValue().equals(Files.readString(path, StandardCharsets.UTF_8))) {
                Files.writeString(path, file.getValue(), StandardCharsets.UTF_8);
                if (bashLaunchers.contains(file.getKey())) {
                    written.add(path);
                }
            }
            if (bashLaunchers.contains(file.getKey())
                    && Files.getFileAttributeView(path, PosixFileAttributeView.class) != null) {
                Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxr-xr-x"));
            }
        }
        return written;
    }

    private LauncherWriter() {
    }
}
