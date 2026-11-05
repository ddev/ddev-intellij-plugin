package de.php_perfect.intellij.ddev.error_reporting;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class PluginVersionTest {
    @Test
    void versionIsExpandedAtBuildTime() {
        final String version = PluginVersion.get();

        Assertions.assertNotNull(version);
        Assertions.assertFalse(version.isBlank());
        Assertions.assertFalse(version.contains("${"), version);
    }
}
