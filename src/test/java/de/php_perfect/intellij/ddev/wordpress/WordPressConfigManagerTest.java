package de.php_perfect.intellij.ddev.wordpress;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class WordPressConfigManagerTest {
    @TempDir
    private Path projectRoot;

    @Test
    void prefersAndUpdatesTheDdevConfig() throws Exception {
        Files.writeString(this.projectRoot.resolve("wp-config.php"), "<?php\ndefine( 'WP_DEBUG', false );\n");
        final Path ddevConfig = this.projectRoot.resolve("wp-config-ddev.php");
        Files.writeString(ddevConfig, "<?php\n\n/**\n * Set WordPress variables.\n */\n");

        assertThat(WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.SILENT))
                .isEqualTo(ddevConfig);
        assertThat(Files.readString(ddevConfig))
                .contains("define( 'WP_DEBUG', true );")
                .contains("define( 'WP_DEBUG_LOG', true );")
                .contains("define( 'WP_DEBUG_DISPLAY', false );");
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(true, true));
    }

    @Test
    void regularDebugRemovesSilentDebugOverrides() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, """
                <?php
                define( 'WP_DEBUG', true );
                define( 'WP_DEBUG_LOG', true );
                define( 'WP_DEBUG_DISPLAY', false );
                /* That's all, stop editing! */
                """);

        WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.ENABLED);

        assertThat(Files.readString(config))
                .contains("define( 'WP_DEBUG', true );")
                .doesNotContain("WP_DEBUG_LOG", "WP_DEBUG_DISPLAY");
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(true, false));
    }

    @Test
    void disablesDebugAndPreservesUnrelatedConfiguration() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, """
                <?php
                define('WP_DEBUG',true);
                define( 'WP_DEBUG_LOG', true );
                define( 'WP_DEBUG_DISPLAY', false );
                define( 'DB_NAME', 'db' );
                """);

        WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.DISABLED);

        assertThat(Files.readString(config))
                .contains("define( 'WP_DEBUG', false );", "define( 'DB_NAME', 'db' );")
                .doesNotContain("WP_DEBUG_LOG", "WP_DEBUG_DISPLAY");
    }

    @Test
    void findsConfigAndDebugLogInTheConfiguredDocumentRoot() throws Exception {
        final Path documentRoot = Files.createDirectories(this.projectRoot.resolve("web"));
        final Path config = documentRoot.resolve("wp-config-ddev.php");
        Files.writeString(config, "<?php\ndefine('WP_DEBUG', false);\n");
        final Path log = documentRoot.resolve("wp-content/debug.log");
        Files.createDirectories(log.getParent());
        Files.writeString(log, "debug output");

        assertThat(WordPressConfigManager.setDebugMode(this.projectRoot, "web", WordPressConfigManager.DebugMode.SILENT))
                .isEqualTo(config);
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot, "web"))
                .isEqualTo(new WordPressConfigManager.DebugState(true, true));
        assertThat(WordPressConfigManager.findFile(this.projectRoot, "web", "wp-content/debug.log")).isEqualTo(log);
        assertThat(this.projectRoot.resolve("wp-config-ddev.php")).doesNotExist();
    }
}
