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
}
