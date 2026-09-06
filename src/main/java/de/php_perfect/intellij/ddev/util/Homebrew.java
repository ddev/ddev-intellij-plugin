package de.php_perfect.intellij.ddev.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Locates a Homebrew installation in its default prefixes on macOS and Linux.
 */
public final class Homebrew {
    private static final @NotNull List<String> BREW_PATHS = List.of(
            "/opt/homebrew/bin/brew",
            "/usr/local/bin/brew",
            "/home/linuxbrew/.linuxbrew/bin/brew"
    );

    private Homebrew() {
    }

    public static @Nullable String find() {
        return BREW_PATHS.stream().filter(path -> Files.isExecutable(Path.of(path))).findFirst().orElse(null);
    }

    /**
     * Returns the brew binary whose prefix holds a DDEV keg, so DDEV can be updated through it.
     */
    public static @Nullable String findManagingDdev() {
        for (final String brewPath : BREW_PATHS) {
            final Path brew = Path.of(brewPath);

            // brew lives at <prefix>/bin/brew; DDEV installed via Homebrew has a keg at <prefix>/Cellar/ddev.
            if (Files.isExecutable(brew) && Files.isDirectory(brew.getParent().getParent().resolve("Cellar").resolve("ddev"))) {
                return brewPath;
            }
        }

        return null;
    }
}
