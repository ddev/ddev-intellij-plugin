package de.php_perfect.intellij.ddev.error_reporting;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * The plugin version, written into a resource at build time.
 */
final class PluginVersion {
    private static final @NotNull String RESOURCE = "/ddev-integration.properties";

    private static final @NotNull String VERSION = load();

    private PluginVersion() {
    }

    static @NotNull String get() {
        return VERSION;
    }

    private static @NotNull String load() {
        try (InputStream stream = PluginVersion.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing resource " + RESOURCE);
            }

            final Properties properties = new Properties();
            properties.load(stream);

            return properties.getProperty("version");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
