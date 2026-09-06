package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class SnapshotFileManagerTest {
    @TempDir
    private Path projectRoot;

    @Test
    void deletesOnlyFilesBelongingToTheSelectedSnapshot() throws Exception {
        final Path snapshots = Files.createDirectories(this.projectRoot.resolve(".ddev/db_snapshots"));
        final Path selected = Files.createFile(snapshots.resolve("before-upgrade-mariadb_11.8.gz"));
        final Path other = Files.createFile(snapshots.resolve("keep-this-mariadb_11.8.gz"));

        assertThat(SnapshotFileManager.deleteSnapshot(this.projectRoot, "before-upgrade")).isTrue();
        assertThat(selected).doesNotExist();
        assertThat(other).exists();
    }

    @Test
    void reportsMissingSnapshotWithoutTouchingTheDirectory() throws Exception {
        final Path snapshots = Files.createDirectories(this.projectRoot.resolve(".ddev/db_snapshots"));

        assertThat(SnapshotFileManager.deleteSnapshot(this.projectRoot, "missing")).isFalse();
        assertThat(snapshots).exists();
    }
}
