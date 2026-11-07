package de.php_perfect.intellij.ddev.docker_compose;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

final class DockerComposeConfigTest {
    private final DockerComposeConfig config = new DockerComposeConfig(List.of("/compose.yaml"), "acol");

    @Test
    void equalConfigsMatch() {
        final DockerComposeConfig other = new DockerComposeConfig(List.of("/compose.yaml"), "acol");

        Assertions.assertEquals(this.config, this.config);
        Assertions.assertEquals(this.config, other);
        Assertions.assertEquals(this.config.hashCode(), other.hashCode());
    }

    @Test
    void differingConfigsDoNotMatch() {
        Assertions.assertNotEquals(null, this.config);
        Assertions.assertNotEquals(new Object(), this.config);
        Assertions.assertNotEquals(new DockerComposeConfig(List.of("/other.yaml"), "acol"), this.config);
        Assertions.assertNotEquals(new DockerComposeConfig(List.of("/compose.yaml"), "other"), this.config);
    }

    @Test
    void toStringListsFields() {
        Assertions.assertEquals("DockerComposeConfig{composeFilePaths=[/compose.yaml], projectName='acol'}", this.config.toString());
    }
}
