package de.php_perfect.intellij.ddev.wordpress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class WordPressShareSupportTest {
    @TempDir
    Path projectRoot;

    @Test
    void configuresAndRestoresShareWithoutDiscardingUnrelatedEdits() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, """
                <?php
                define( 'WP_HOME', 'https://custom.example' );
                /* That's all, stop editing! */
                """);

        final WordPressShareSupport.Session session = WordPressShareSupport.start(
                this.projectRoot, "https://public.ngrok.app");

        assertThat(session).isNotNull();
        assertThat(config).content().contains(
                "define( 'WP_SHARED_URL', 'https://public.ngrok.app' );",
                "$_SERVER['HTTP_HOST']",
                "define( 'WP_SITEURL', WP_HOME . '/' );");
        final Path plugin = this.projectRoot.resolve("wp-content/mu-plugins/ddev-intellij-share.php");
        assertThat(plugin).content().contains("DDEV_HOSTNAME", "WP_SHARED_URL");

        Files.writeString(config, Files.readString(config) + "\ndefine( 'UNRELATED', true );\n");
        session.close();

        assertThat(config).content()
                .contains("define( 'WP_HOME', 'https://custom.example' );")
                .contains("define( 'UNRELATED', true );")
                .doesNotContain("WP_SHARED_URL", "WP_SITEURL");
        assertThat(plugin).doesNotExist();
    }

    @Test
    void ignoresProjectsWithoutAWordPressConfig() throws Exception {
        assertThat(WordPressShareSupport.start(this.projectRoot, "https://public.ngrok.app")).isNull();
    }

    @Test
    void restoresAnExistingPluginFile() throws Exception {
        Files.writeString(this.projectRoot.resolve("wp-config.php"), "<?php\n");
        final Path plugin = this.projectRoot.resolve("wp-content/mu-plugins/ddev-intellij-share.php");
        Files.createDirectories(plugin.getParent());
        Files.writeString(plugin, "existing");

        final WordPressShareSupport.Session session = WordPressShareSupport.start(
                this.projectRoot, "https://public.ngrok.app");
        session.close();

        assertThat(plugin).hasContent("existing");
    }
}
