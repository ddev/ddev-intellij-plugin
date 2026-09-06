package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Records which IDE windows use a DDEV project, across all JetBrains IDE processes of the user, so
 * a project is only stopped automatically once the last window using it closes.
 */
final class ProjectLocks {
    private static final Logger LOG = Logger.getInstance(ProjectLocks.class);

    private final @NotNull Path locksDirectory;

    ProjectLocks(@NotNull Path locksDirectory) {
        this.locksDirectory = locksDirectory;
    }

    static @NotNull ProjectLocks shared() {
        return new ProjectLocks(Path.of(System.getProperty("java.io.tmpdir"), "ddev-intellij-plugin", "locks"));
    }

    void acquire(@NotNull String projectRoot, @NotNull Project project) {
        this.acquire(projectRoot, project.getLocationHash(), ProcessHandle.current().pid());
    }

    /**
     * Removes this window's lock and reports whether another live window still uses the project.
     */
    boolean releaseAndCheckInUse(@NotNull String projectRoot, @NotNull Project project) {
        return this.releaseAndCheckInUse(projectRoot, project.getLocationHash(), ProcessHandle.current().pid());
    }

    void acquire(@NotNull String projectRoot, @NotNull String window, long pid) {
        final Path directory = this.directory(projectRoot);
        try {
            Files.createDirectories(directory);
            Files.writeString(directory.resolve(lockName(window, pid)), projectRoot, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            LOG.info("Could not record the DDEV project lock", exception);
        }
    }

    boolean releaseAndCheckInUse(@NotNull String projectRoot, @NotNull String window, long pid) {
        final Path directory = this.directory(projectRoot);
        boolean inUse = false;

        try {
            Files.deleteIfExists(directory.resolve(lockName(window, pid)));
            if (!Files.isDirectory(directory)) {
                return false;
            }
            try (DirectoryStream<Path> locks = Files.newDirectoryStream(directory, "*.lock")) {
                for (Path lock : locks) {
                    if (isAlive(lock)) {
                        inUse = true;
                    } else {
                        Files.deleteIfExists(lock);
                    }
                }
            }
        } catch (IOException exception) {
            LOG.info("Could not read the DDEV project locks", exception);
            // Keeping the project running is the safe outcome when other windows cannot be ruled out.
            return true;
        }

        return inUse;
    }

    private static boolean isAlive(@NotNull Path lock) {
        final String name = lock.getFileName().toString();
        try {
            final long pid = Long.parseLong(name.substring(0, name.indexOf('-')));
            return ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private @NotNull Path directory(@NotNull String projectRoot) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(Path.of(projectRoot).toAbsolutePath().normalize().toString().getBytes(StandardCharsets.UTF_8));
            return this.locksDirectory.resolve(HexFormat.of().formatHex(digest, 0, 16));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static @NotNull String lockName(@NotNull String window, long pid) {
        return pid + "-" + window.replaceAll("[^A-Za-z0-9_.]", "_") + ".lock";
    }
}
