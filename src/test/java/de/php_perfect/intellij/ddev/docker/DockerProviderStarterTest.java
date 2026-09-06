package de.php_perfect.intellij.ddev.docker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

final class DockerProviderStarterTest {
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void findsExecutablesOnThePath() {
        assertThat(DockerProviderStarter.findBinary("sh")).endsWith("/sh");
        assertThat(DockerProviderStarter.findBinary("definitely-not-an-installed-binary")).isNull();
    }
}
