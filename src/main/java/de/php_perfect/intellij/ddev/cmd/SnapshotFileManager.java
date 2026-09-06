package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

/** File-level operation needed because DDEV can clean all snapshots but cannot delete one snapshot. */
public final class SnapshotFileManager {
    private SnapshotFileManager() {
    }

    /** A snapshot is stored as {@code <name>-<database type>_<version>.gz} in {@code .ddev/db_snapshots}. */
    public static boolean deleteSnapshot(@NotNull Path projectRoot, @NotNull String name) throws IOException {
        final Path snapshotDir = projectRoot.resolve(".ddev").resolve("db_snapshots");
        final Pattern filePattern = Pattern.compile(Pattern.quote(name) + "-(mysql|mariadb|postgres)_.+");
        boolean deleted = false;

        if (!Files.isDirectory(snapshotDir)) {
            return false;
        }

        try (DirectoryStream<Path> entries = Files.newDirectoryStream(snapshotDir)) {
            for (final Path entry : entries) {
                if (Files.isRegularFile(entry) && filePattern.matcher(entry.getFileName().toString()).matches()) {
                    Files.delete(entry);
                    deleted = true;
                }
            }
        }

        return deleted;
    }
}
