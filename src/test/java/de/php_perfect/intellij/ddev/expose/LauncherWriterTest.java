package de.php_perfect.intellij.ddev.expose;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

final class LauncherWriterTest {
    @TempDir
    private Path directory;

    @Test
    void windowsGetsCmdWrappersAPowerShellLauncherAndGitBashLaunchers() throws Exception {
        LauncherWriter.write(this.directory, LauncherWriter.Target.WINDOWS, "site", "C:\\Users\\me\\site",
                "C:\\Program Files\\DDEV\\ddev.exe", List.of(new ExposedCommands.Launcher("wp", "wp")), true);

        assertThat(names()).containsExactlyInAnyOrder("wp", "wp.cmd", "ddev-exposed.ps1");
        assertThat(Files.readString(this.directory.resolve("wp.cmd")))
                .contains("-File \"%~dp0ddev-exposed.ps1\" wp %*\r\n");
        assertThat(Files.readString(this.directory.resolve("ddev-exposed.ps1")))
                .contains("$approot = 'C:\\Users\\me\\site'\r\n", "'exec', '--quiet', '--raw'");
        assertThat(Files.readString(this.directory.resolve("wp")))
                .contains("approot='C:/Users/me/site'\n", "ddev='C:/Program Files/DDEV/ddev.exe'\n");
    }

    @Test
    void removesLaunchersThatAreNoLongerConfigured() throws Exception {
        LauncherWriter.write(this.directory, LauncherWriter.Target.UNIX, "site", "/site", "/usr/bin/ddev",
                List.of(new ExposedCommands.Launcher("wp", "wp"), new ExposedCommands.Launcher("drush", "drush")), true);
        final List<Path> written = LauncherWriter.write(this.directory, LauncherWriter.Target.UNIX, "site", "/site",
                "/usr/bin/ddev", List.of(new ExposedCommands.Launcher("wp", "wp")), true);

        assertThat(names()).containsExactly("wp");
        assertThat(written).as("unchanged launchers are not rewritten").isEmpty();
    }

    private List<String> names() throws Exception {
        try (var files = Files.list(this.directory)) {
            return files.map(file -> file.getFileName().toString()).toList();
        }
    }
}
