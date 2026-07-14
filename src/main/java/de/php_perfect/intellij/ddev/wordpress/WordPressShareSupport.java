package de.php_perfect.intellij.ddev.wordpress;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        final Path config = WordPressConfigManager.findConfig(projectRoot);
        if (config == null) {
            return null;
        }

        final String originalConfig = Files.readString(config);
        final Map<String, List<String>> originalDefinitions = captureDefinitions(originalConfig);
        String updatedConfig = originalConfig;
        updatedConfig = setDefinition(updatedConfig, "WP_SHARED_URL",
                "define( 'WP_SHARED_URL', '" + escapePhpString(shareUrl) + "' );");
        updatedConfig = setDefinition(updatedConfig, "WP_HOME",
                "define( 'WP_HOME', isset( $_SERVER['HTTP_HOST'] ) ? 'https://' . $_SERVER['HTTP_HOST'] : WP_SHARED_URL );");
        updatedConfig = setDefinition(updatedConfig, "WP_SITEURL",
                "define( 'WP_SITEURL', WP_HOME . '/' );");

        final Path plugin = projectRoot.resolve("wp-content/mu-plugins/ddev-intellij-share.php");
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

        return new Session(config, originalDefinitions, plugin, pluginExisted, originalPlugin);
    }

    private static @NotNull Map<String, List<String>> captureDefinitions(@NotNull String content) {
        final LinkedHashMap<String, List<String>> definitions = new LinkedHashMap<>();
        for (String name : SHARE_CONSTANTS) {
            final ArrayList<String> matches = new ArrayList<>();
            final Matcher matcher = definitionPattern(name).matcher(content);
            while (matcher.find()) {
                matches.add(matcher.group().stripTrailing());
            }
            definitions.put(name, matches);
        }
        return definitions;
    }

    private static @NotNull String setDefinition(@NotNull String content, @NotNull String name,
                                                  @NotNull String definition) {
        return insertDefinition(definitionPattern(name).matcher(content).replaceAll(""), definition);
    }

    private static @NotNull Pattern definitionPattern(@NotNull String name) {
        return Pattern.compile("(?m)^[\\t ]*define\\s*\\(\\s*(['\"])" + Pattern.quote(name)
                + "\\1\\s*,\\s*[^;\\r\\n]+?\\s*\\)\\s*;[\\t ]*(?:\\R|$)");
    }

    private static @NotNull String insertDefinition(@NotNull String content, @NotNull String definition) {
        final String newline = content.contains("\r\n") ? "\r\n" : "\n";
        final Matcher anchor = Pattern.compile("(?m)^[\\t ]*/\\*(?:\\*|\\s+That's all)").matcher(content);
        if (anchor.find()) {
            return content.substring(0, anchor.start()) + definition + newline + newline
                    + content.substring(anchor.start());
        }
        final int phpClose = content.lastIndexOf("?>");
        final int insertionPoint = phpClose >= 0 ? phpClose : content.length();
        final String separator = insertionPoint > 0 && !content.substring(0, insertionPoint).endsWith(newline)
                ? newline : "";
        return content.substring(0, insertionPoint) + separator + definition + newline
                + content.substring(insertionPoint);
    }

    private static @NotNull String escapePhpString(@NotNull String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }

    public static final class Session implements AutoCloseable {
        private final @NotNull Path config;
        private final @NotNull Map<String, List<String>> originalDefinitions;
        private final @NotNull Path plugin;
        private final boolean pluginExisted;
        private final byte @Nullable [] originalPlugin;
        private boolean closed;

        private Session(@NotNull Path config, @NotNull Map<String, List<String>> originalDefinitions,
                        @NotNull Path plugin, boolean pluginExisted, byte @Nullable [] originalPlugin) {
            this.config = config;
            this.originalDefinitions = originalDefinitions;
            this.plugin = plugin;
            this.pluginExisted = pluginExisted;
            this.originalPlugin = originalPlugin;
        }

        @Override
        public synchronized void close() throws IOException {
            if (this.closed) {
                return;
            }
            this.closed = true;

            if (Files.isRegularFile(this.config)) {
                String content = Files.readString(this.config);
                for (String name : SHARE_CONSTANTS) {
                    content = definitionPattern(name).matcher(content).replaceAll("");
                    for (String definition : this.originalDefinitions.getOrDefault(name, List.of())) {
                        content = insertDefinition(content, definition);
                    }
                }
                Files.writeString(this.config, content, StandardCharsets.UTF_8);
            }

            if (this.pluginExisted && this.originalPlugin != null) {
                Files.write(this.plugin, this.originalPlugin);
            } else {
                Files.deleteIfExists(this.plugin);
            }
        }
    }
}
