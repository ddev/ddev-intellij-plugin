package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** File-level operation needed because DDEV can clean all snapshots but cannot delete one snapshot. */
public final class SnapshotFileManager {
    private SnapshotFileManager() {
    }

    public static boolean deleteSnapshot(@NotNull Path projectRoot, @NotNull String name) throws IOException {
        final Path snapshotDir = projectRoot.resolve(".ddev").resolve("db_snapshots");
        final Pattern filePattern = Pattern.compile(Pattern.quote(name) + "-(mysql|mariadb|postgres)_.+");
        boolean deleted = false;

        if (!Files.isDirectory(snapshotDir)) {
            return false;
        }

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(snapshotDir)) {
            for (final Path entry : entries) {
                final String fileName = entry.getFileName().toString();

                if (fileName.equals(name) || filePattern.matcher(fileName).matches()) {
                    deleteRecursively(entry);
                    deleted = true;
                }
            }
        }

        return deleted;
    }

    private static void deleteRecursively(@NotNull Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            for (final Path entry : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(entry);
            }
        }
    }
}
