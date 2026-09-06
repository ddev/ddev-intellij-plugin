package de.php_perfect.intellij.ddev.util;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

/**
 * Resolves the DDEV project an IDE project belongs to. The IDE project may be opened at the DDEV
 * project root or at any folder below it, as DDEV itself finds its project from subfolders.
 */
public final class DdevProjectRoot {
    private static final @NotNull String CONFIG_FILE = ".ddev/config.yaml";

    private DdevProjectRoot() {
    }

    /**
     * Returns the directory holding {@code .ddev/config.yaml} at or above the IDE project's base path,
     * or the base path itself when no DDEV project encloses it.
     */
    public static @Nullable String of(@NotNull Project project) {
        final String basePath = project.getBasePath();
        return basePath == null ? null : find(basePath);
    }

    public static @NotNull String find(@NotNull String basePath) {
        final Path start;
        try {
            start = Path.of(basePath).toAbsolutePath().normalize();
        } catch (InvalidPathException exception) {
            return basePath;
        }

        // The home directory's own .ddev holds DDEV's global configuration, not a project.
        final Path home = Path.of(System.getProperty("user.home")).toAbsolutePath().normalize();
        for (Path directory = start; directory != null && !directory.equals(home); directory = directory.getParent()) {
            if (Files.isRegularFile(directory.resolve(CONFIG_FILE))) {
                return directory.equals(start) ? basePath : directory.toString();
            }
        }

        return basePath;
    }
}
