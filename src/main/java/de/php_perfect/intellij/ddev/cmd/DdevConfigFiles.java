package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DdevConfigFiles {
    private static final String PHP_TEMPLATE = """
            ; Custom PHP configuration for this DDEV project.
            ; Settings in this file are applied to the web container.
            ; Run "ddev restart" after changing this file.
            ;
            ; Example:
            ; [PHP]
            ; memory_limit = 512M
            """;

    private DdevConfigFiles() {
    }

    public static @NotNull Path ensureCustomPhpIni(@NotNull Path projectRoot) throws IOException {
        final Path ini = projectRoot.resolve(".ddev/php/custom-php.ini");

        if (!Files.exists(ini)) {
            Files.createDirectories(ini.getParent());
            Files.writeString(ini, PHP_TEMPLATE);
        }

        return ini;
    }

    public static @Nullable Path findWebserverConfig(@NotNull Path projectRoot) {
        final Path nginx = projectRoot.resolve(".ddev/nginx_full/nginx-site.conf");

        if (Files.isRegularFile(nginx)) {
            return nginx;
        }

        final Path apache = projectRoot.resolve(".ddev/apache/apache-site.conf");
        return Files.isRegularFile(apache) ? apache : null;
    }
}
