package de.php_perfect.intellij.ddev.terminal;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevTerminalCustomizerTest {
    @Test
    void prependsToExistingPath() {
        final Map<String, String> envs = new HashMap<>(Map.of("PATH", "/usr/bin"));

        DdevTerminalCustomizer.prependToPath(envs, "/launchers");

        assertThat(envs).containsEntry("PATH", "/launchers" + File.pathSeparator + "/usr/bin");
    }

    @Test
    void keepsTheWindowsSpellingOfPath() {
        final Map<String, String> envs = new HashMap<>(Map.of("Path", "C:\\Windows"));

        DdevTerminalCustomizer.prependToPath(envs, "C:\\launchers");

        assertThat(envs).containsOnlyKeys("Path").containsEntry("Path", "C:\\launchers" + File.pathSeparator + "C:\\Windows");
    }

    @Test
    void setsPathWhenMissing() {
        final Map<String, String> envs = new HashMap<>();

        DdevTerminalCustomizer.prependToPath(envs, "/launchers");

        assertThat(envs).containsEntry("PATH", "/launchers");
    }
}
