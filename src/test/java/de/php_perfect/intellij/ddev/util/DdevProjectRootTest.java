package de.php_perfect.intellij.ddev.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevProjectRootTest {
    @TempDir
    private Path temp;

    @Test
    void findsTheDdevProjectEnclosingASubfolder() throws Exception {
        final Path projectRoot = this.temp.resolve("site");
        Files.createDirectories(projectRoot.resolve(".ddev"));
        Files.writeString(projectRoot.resolve(".ddev/config.yaml"), "name: site\n");
        final Path theme = Files.createDirectories(projectRoot.resolve("web/wp-content/themes/custom"));

        assertThat(DdevProjectRoot.find(theme.toString())).isEqualTo(projectRoot.toString());
        assertThat(DdevProjectRoot.find(projectRoot.toString())).isEqualTo(projectRoot.toString());
    }

    @Test
    void keepsTheBasePathOutsideOfADdevProject() throws Exception {
        final Path folder = Files.createDirectories(this.temp.resolve("plain/folder"));
        // A .ddev directory without config.yaml is not a project.
        Files.createDirectories(this.temp.resolve("plain/.ddev"));

        assertThat(DdevProjectRoot.find(folder.toString())).isEqualTo(folder.toString());
    }
}
