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
    void overridesTheDdevDefaultsInWpConfigAndLeavesTheGeneratedFileAlone() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, """
                <?php
                /**
                 * #ddev-generated: Automatically generated WordPress settings file.
                 * ddev manages this file and may delete or overwrite the file unless this comment is removed.
                 * It is recommended that you leave this file alone.
                 *
                 * @package ddevapp
                 */

                /* Add any custom values between this line and the "stop editing" line. */

                /* That's all, stop editing! Happy publishing. */

                $ddev_settings = __DIR__ . '/wp-config-ddev.php';
                require_once $ddev_settings;
                """);
        final Path ddevConfig = this.projectRoot.resolve("wp-config-ddev.php");
        final String generated = """
                <?php
                /**
                 * #ddev-generated: Automatically generated WordPress settings file.
                 */
                defined( 'WP_HOME' ) || define( 'WP_HOME', 'https://old.ddev.site' );
                defined( 'WP_DEBUG' ) || define( 'WP_DEBUG', true );
                """;
        Files.writeString(ddevConfig, generated);

        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(true, false));

        assertThat(WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.SILENT))
                .isEqualTo(config);
        final String content = Files.readString(config);
        assertThat(content)
                .doesNotContain("#ddev-generated", "ddev manages this file", "leave this file alone")
                .contains("define( 'WP_DEBUG', true );", "define( 'WP_DEBUG_LOG', true );",
                        "define( 'WP_DEBUG_DISPLAY', false );");
        assertThat(content.indexOf("WP_DEBUG_DISPLAY")).isLessThan(content.indexOf("That's all"));
        assertThat(Files.readString(ddevConfig)).isEqualTo(generated);
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(true, true));

        WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.DISABLED);
        assertThat(Files.readString(ddevConfig)).isEqualTo(generated);
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(false, false));
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
                .contains("define('WP_DEBUG',false);", "define( 'DB_NAME', 'db' );")
                .doesNotContain("WP_DEBUG_LOG", "WP_DEBUG_DISPLAY");
    }

    @Test
    void keepsTheGuardOfAGuardedDefinition() throws Exception {
        final Path config = this.projectRoot.resolve("wp-config.php");
        Files.writeString(config, """
                <?php
                if (true) {
                    defined('WP_DEBUG') || define('WP_DEBUG', true);
                    $table_prefix = 'wp_';
                }
                """);

        WordPressConfigManager.setDebugMode(this.projectRoot, WordPressConfigManager.DebugMode.DISABLED);

        assertThat(Files.readString(config))
                .contains("    defined('WP_DEBUG') || define('WP_DEBUG', false);\n    $table_prefix = 'wp_';");
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot))
                .isEqualTo(new WordPressConfigManager.DebugState(false, false));
    }

    @Test
    void findsConfigAndDebugLogInTheConfiguredDocumentRoot() throws Exception {
        final Path documentRoot = Files.createDirectories(this.projectRoot.resolve("web"));
        final Path config = documentRoot.resolve("wp-config.php");
        Files.writeString(config, "<?php\ndefine('WP_DEBUG', false);\n");
        final Path log = documentRoot.resolve("wp-content/debug.log");
        Files.createDirectories(log.getParent());
        Files.writeString(log, "debug output");

        assertThat(WordPressConfigManager.setDebugMode(this.projectRoot, "web", WordPressConfigManager.DebugMode.SILENT))
                .isEqualTo(config);
        assertThat(WordPressConfigManager.readDebugState(this.projectRoot, "web"))
                .isEqualTo(new WordPressConfigManager.DebugState(true, true));
        assertThat(WordPressConfigManager.findFile(this.projectRoot, "web", "wp-content/debug.log")).isEqualTo(log);
        assertThat(this.projectRoot.resolve("wp-config.php")).doesNotExist();
    }
}
