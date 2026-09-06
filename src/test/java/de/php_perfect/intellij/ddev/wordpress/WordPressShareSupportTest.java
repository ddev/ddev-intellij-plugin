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

    @Test
    void restoresConditionalDefinitionsInPlaceWithOriginalLineEndings() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        final String original = "<?php\r\n/** Site configuration. */\r\ndeclare(strict_types=1);\r\n"
                + "if (getenv('IS_DDEV_PROJECT') === 'true') {\r\n"
                + "    define( 'WP_HOME', 'https://local.ddev.site' );\r\n"
                + "} else {\r\n    define( 'WP_HOME', 'https://production.example' );\r\n}\r\n"
                + "require_once ABSPATH . 'wp-settings.php';\r\n";
        Files.writeString(config, original);
        try (var session = WordPressShareSupport.start(this.projectRoot, "https://public.ngrok.app")) {
            assertThat(session).isNotNull();
            final String sharing = Files.readString(config);
            assertThat(sharing.indexOf("define( 'WP_SHARED_URL'"))
                    .isLessThan(sharing.indexOf("require_once"))
                    .isGreaterThan(sharing.indexOf("declare(strict_types=1);"));
            Files.writeString(config, sharing + "// unrelated edit\r\n");
        }
        assertThat(config).content().isEqualTo(original + "// unrelated edit\r\n");
    }

    @Test
    void sharesFromTheDocumentRootAndSupportsAParentConfig() throws Exception {
        final Path documentRoot = Files.createDirectories(this.projectRoot.resolve("public"));
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, "<?php\n");
        try (var session = WordPressShareSupport.start(this.projectRoot, "public", "https://public.ngrok.app")) {
            assertThat(session).isNotNull();
            assertThat(documentRoot.resolve("wp-content/mu-plugins/ddev-intellij-share.php")).exists();
            assertThat(this.projectRoot.resolve("wp-content")).doesNotExist();
        }
        assertThat(config).hasContent("<?php\n");
        assertThat(documentRoot.resolve("wp-content/mu-plugins/ddev-intellij-share.php")).doesNotExist();
    }
}
