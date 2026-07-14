package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevProjectFilesTest {
    @TempDir
    private Path projectRoot;

    @Test
    void deletesOnlyTheDdevConfigurationTree() throws Exception {
        final Path projectFile = Files.createFile(this.projectRoot.resolve("index.php"));
        final Path ddevFile = Files.createDirectories(this.projectRoot.resolve(".ddev/nginx_full"))
                .resolve("nginx-site.conf");
        Files.createFile(ddevFile);

        assertThat(DdevProjectFiles.deleteDdevConfig(this.projectRoot)).isTrue();
        assertThat(this.projectRoot.resolve(".ddev")).doesNotExist();
        assertThat(projectFile).exists();
    }

    @Test
    void reportsAnAlreadyMissingConfiguration() throws Exception {
        assertThat(DdevProjectFiles.deleteDdevConfig(this.projectRoot)).isFalse();
    }
}
