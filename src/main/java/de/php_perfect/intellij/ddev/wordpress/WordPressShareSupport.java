package de.php_perfect.intellij.ddev.wordpress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Makes WordPress output use a temporary DDEV share URL without changing database content. */
public final class WordPressShareSupport {
    private static final List<String> SHARE_CONSTANTS = List.of("WP_SHARED_URL", "WP_HOME", "WP_SITEURL");
    private static final String SHARE_PLUGIN = """
            <?php
            /** Temporary DDEV share URL rewriting, managed by the DDEV Integration plugin. */
            if ( ! defined( 'WP_SHARED_URL' ) ) {
                return;
            }

            add_action( 'template_redirect', static function (): void {
                ob_start( static function ( string $html ): string {
                    $local_host = $_SERVER['DDEV_HOSTNAME'] ?? '';
                    if ( '' === $local_host ) {
                        return $html;
                    }

                    $share_url = rtrim( WP_SHARED_URL, '/' );
                    return str_replace(
                        [
                            'https://' . $local_host,
                            'https://www.' . $local_host,
                            'http://' . $local_host,
                            'http://www.' . $local_host,
                        ],
                        $share_url,
                        $html
                    );
                } );
            }, 0 );
            """;

    private WordPressShareSupport() {
    }

    public static @Nullable Session start(@NotNull Path projectRoot, @NotNull String shareUrl)
            throws IOException {
        return start(projectRoot, null, shareUrl);
    }

    public static @Nullable Session start(@NotNull Path projectRoot, @Nullable String docroot,
                                          @NotNull String shareUrl) throws IOException {
        final Path config = WordPressConfigManager.findConfig(projectRoot, docroot);
        if (config == null) {
            return null;
        }

        final String originalConfig = Files.readString(config);
        final String newline = originalConfig.contains("\r\n") ? "\r\n" : "\n";
        final String marker = "DDEV share " + UUID.randomUUID();
        final Map<String, String> replacements = new LinkedHashMap<>();
        String updatedConfig = originalConfig;
        for (String name : SHARE_CONSTANTS) {
            updatedConfig = definitionPattern(name).matcher(updatedConfig).replaceAll(match -> {
                final String placeholder = "/* " + marker + ":" + replacements.size() + " */;" + newline;
                replacements.put(placeholder, match.group());
                return Matcher.quoteReplacement(placeholder);
            });
        }
        final Matcher openingTag = Pattern.compile("(?is)<\\?php\\b"
                + "(?:(?:\\s|/\\*.*?\\*/|//[^\\r\\n]*|#[^\\r\\n]*)*declare\\s*\\([^)]*\\)\\s*;)*")
                .matcher(updatedConfig);
        if (!openingTag.find()) {
            throw new IOException("Missing PHP opening tag in " + config);
        }
        final String definitions = newline + "/* " + marker + " */" + newline
                + "define( 'WP_SHARED_URL', '" + escapePhpString(shareUrl) + "' );" + newline
                + "define( 'WP_HOME', isset( $_SERVER['HTTP_HOST'] ) ? 'https://' . $_SERVER['HTTP_HOST'] : WP_SHARED_URL );" + newline
                + "define( 'WP_SITEURL', WP_HOME . '/' );" + newline;
        replacements.put(definitions, "");
        updatedConfig = updatedConfig.substring(0, openingTag.end()) + definitions
                + updatedConfig.substring(openingTag.end());

        final Path plugin = WordPressConfigManager.documentRoot(projectRoot, docroot)
                .resolve("wp-content/mu-plugins/ddev-intellij-share.php");
        final boolean pluginExisted = Files.exists(plugin);
        final byte[] originalPlugin = pluginExisted ? Files.readAllBytes(plugin) : null;

        Files.writeString(config, updatedConfig, StandardCharsets.UTF_8);
        try {
            Files.createDirectories(plugin.getParent());
            Files.writeString(plugin, SHARE_PLUGIN, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            Files.writeString(config, originalConfig, StandardCharsets.UTF_8);
            throw exception;
        }

        return new Session(config, replacements, plugin, pluginExisted, originalPlugin);
    }

    private static @NotNull Pattern definitionPattern(@NotNull String name) {
        return Pattern.compile("(?m)^[\\t ]*define\\s*\\(\\s*(['\"])" + Pattern.quote(name)
                + "\\1\\s*,\\s*[^;\\r\\n]+?\\s*\\)\\s*;[\\t ]*(?:\\R|$)");
    }

    private static @NotNull String escapePhpString(@NotNull String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static final class Session implements AutoCloseable {
        private final @NotNull Path config;
        private final @NotNull Map<String, String> replacements;
        private final @NotNull Path plugin;
        private final boolean pluginExisted;
        private final byte @Nullable [] originalPlugin;
        private boolean closed;

        private Session(@NotNull Path config, @NotNull Map<String, String> replacements,
                        @NotNull Path plugin, boolean pluginExisted, byte @Nullable [] originalPlugin) {
            this.config = config;
            this.replacements = replacements;
            this.plugin = plugin;
            this.pluginExisted = pluginExisted;
            this.originalPlugin = originalPlugin;
        }

        @Override
        public synchronized void close() throws IOException {
            if (this.closed) {
                return;
            }
            if (Files.isRegularFile(this.config)) {
                String content = Files.readString(this.config);
                for (Map.Entry<String, String> replacement : this.replacements.entrySet()) {
                    content = content.replace(replacement.getKey(), replacement.getValue());
                }
                Files.writeString(this.config, content, StandardCharsets.UTF_8);
            }

            if (this.pluginExisted && this.originalPlugin != null) {
                Files.write(this.plugin, this.originalPlugin);
            } else {
                Files.deleteIfExists(this.plugin);
            }
            this.closed = true;
        }
    }
}
