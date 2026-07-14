package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

public final class DdevProjectFiles {
    private DdevProjectFiles() {
    }

    public static boolean deleteDdevConfig(@NotNull Path projectRoot) throws IOException {
        final Path ddevDirectory = projectRoot.resolve(".ddev");

        if (!Files.exists(ddevDirectory)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(ddevDirectory)) {
            for (final Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }

        return true;
    }
}
