package de.php_perfect.intellij.ddev.cms;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class CmsProjectInstallerTest {
    @TempDir
    Path projectRoot;

    @Test
    void movesGeneratedProjectContentsWithoutReplacingDdevConfiguration() throws Exception {
        Files.createDirectories(this.projectRoot.resolve(".ddev"));
        Files.createDirectories(this.projectRoot.resolve("generated/src"));
        Files.writeString(this.projectRoot.resolve("generated/package.json"), "{}");
        Files.writeString(this.projectRoot.resolve("generated/src/index.js"), "export {};");

        CmsProjectInstaller.moveProjectContents(this.projectRoot, "generated");

        assertThat(this.projectRoot.resolve("package.json")).hasContent("{}");
        assertThat(this.projectRoot.resolve("src/index.js")).hasContent("export {};");
        assertThat(this.projectRoot.resolve(".ddev")).isDirectory();
        assertThat(this.projectRoot.resolve("generated")).doesNotExist();
    }

    @Test
    void rejectsGeneratedProjectConflicts() throws Exception {
        Files.createDirectories(this.projectRoot.resolve("generated"));
        Files.writeString(this.projectRoot.resolve("generated/package.json"), "new");
        Files.writeString(this.projectRoot.resolve("package.json"), "existing");

        assertThatThrownBy(() -> CmsProjectInstaller.moveProjectContents(this.projectRoot, "generated"))
                .isInstanceOf(java.io.IOException.class)
                .hasMessageContaining("conflicts");
        assertThat(this.projectRoot.resolve("package.json")).hasContent("existing");
    }

    @Test
    void rejectsFileWritesOutsideProject() {
        assertThatThrownBy(() -> CmsProjectInstaller.writeProjectFile(this.projectRoot,
                new CmsInstallationRecipe.WriteFile("../outside", "no")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
