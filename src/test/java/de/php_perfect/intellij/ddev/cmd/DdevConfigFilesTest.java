package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevConfigFilesTest {
    @TempDir
    private Path projectRoot;

    @Test
    void createsAReusableCustomPhpConfiguration() throws Exception {
        final Path ini = DdevConfigFiles.ensureCustomPhpIni(this.projectRoot);

        assertThat(ini).exists();
        assertThat(Files.readString(ini)).contains("memory_limit = 512M");
        Files.writeString(ini, "memory_limit = 1G\n");
        assertThat(DdevConfigFiles.ensureCustomPhpIni(this.projectRoot)).hasContent("memory_limit = 1G\n");
    }

    @Test
    void prefersNginxWhenBothGeneratedConfigsExist() throws Exception {
        final Path nginx = Files.createDirectories(this.projectRoot.resolve(".ddev/nginx_full"))
                .resolve("nginx-site.conf");
        Files.createFile(nginx);
        final Path apache = Files.createDirectories(this.projectRoot.resolve(".ddev/apache"))
                .resolve("apache-site.conf");
        Files.createFile(apache);

        assertThat(DdevConfigFiles.findWebserverConfig(this.projectRoot)).isEqualTo(nginx);
    }
}
