package de.php_perfect.intellij.ddev.error_reporting;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

final class PluginVersionTest {
    @Test
    void versionIsExpandedAtBuildTime() {
        final String version = PluginVersion.get();

        Assertions.assertNotNull(version);
        Assertions.assertFalse(version.isBlank());
        Assertions.assertFalse(version.contains("${"), version);
    }

    @Test
    void missingResourceFails() {
        final Throwable cause = initializationFailure(null);

        Assertions.assertInstanceOf(IllegalStateException.class, cause);
        Assertions.assertEquals("Missing resource /ddev-integration.properties", cause.getMessage());
    }

    @Test
    void unreadableResourceFails() {
        final InputStream unreadable = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("unreadable");
            }
        };

        Assertions.assertInstanceOf(UncheckedIOException.class, initializationFailure(unreadable));
    }

    @Test
    void resourceWithoutVersionFails() {
        final InputStream empty = new ByteArrayInputStream("name=ddev\n".getBytes(StandardCharsets.UTF_8));

        Assertions.assertInstanceOf(IllegalStateException.class, initializationFailure(empty));
    }

    private static Throwable initializationFailure(@Nullable InputStream resource) {
        final ClassLoader loader = new ResourceOverridingClassLoader(resource);
        final String className = PluginVersion.class.getName();
        final ExceptionInInitializerError error = Assertions.assertThrows(ExceptionInInitializerError.class,
                () -> Class.forName(className, true, loader));

        return error.getCause();
    }

    /**
     * Loads a private PluginVersion copy reading the given resource.
     */
    private static final class ResourceOverridingClassLoader extends ClassLoader {
        private final @Nullable InputStream resource;

        private ResourceOverridingClassLoader(@Nullable InputStream resource) {
            super(PluginVersion.class.getClassLoader());
            this.resource = resource;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.equals(PluginVersion.class.getName())) {
                return super.loadClass(name, resolve);
            }

            synchronized (getClassLoadingLock(name)) {
                final Class<?> loaded = findLoadedClass(name);
                if (loaded != null) {
                    return loaded;
                }

                try (InputStream stream = PluginVersion.class.getResourceAsStream(PluginVersion.class.getSimpleName() + ".class")) {
                    final byte[] bytes = stream.readAllBytes();
                    return defineClass(name, bytes, 0, bytes.length, PluginVersion.class.getProtectionDomain());
                } catch (IOException exception) {
                    throw new ClassNotFoundException(name, exception);
                }
            }
        }

        @Override
        public @Nullable InputStream getResourceAsStream(String name) {
            return this.resource;
        }
    }
}
