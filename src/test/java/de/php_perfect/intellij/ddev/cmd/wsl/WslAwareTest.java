package de.php_perfect.intellij.ddev.cmd.wsl;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.wsl.WSLCommandLineOptions;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

final class WslAwareTest {
    @Test
    void leavesHostCommandsWithoutAWorkingDirectoryAlone() {
        final var command = new GeneralCommandLine("brew", "upgrade", "ddev/ddev/ddev");
        assertThat(WslAware.patchCommandLine(command)).isSameAs(command);
        assertThat(command.getWorkDirectory()).isNull();
    }

    @Test
    void convertsSelectedProjectAndFilePathsUsingTheProjectsDistribution() throws Exception {
        final String base = "\\\\wsl.localhost\\Ubuntu\\home\\user\\current";
        final String selected = "\\\\wsl.localhost\\Ubuntu\\home\\user\\other";
        final var distribution = mock(WSLDistribution.class);
        try (var paths = mockStatic(WslPath.class)) {
            paths.when(() -> WslPath.getDistributionByWindowsUncPath(base)).thenReturn(distribution);
            paths.when(() -> WslPath.getDistributionByWindowsUncPath(selected)).thenReturn(distribution);
            paths.when(() -> WslPath.parseWindowsUncPath(base))
                    .thenReturn(new WslPath("\\\\wsl.localhost\\", "Ubuntu", "/home/user/current"));
            when(distribution.getWslPath(Path.of("C:/backups/db.sql"))).thenReturn("/mnt/c/backups/db.sql");
            final var command = new GeneralCommandLine("ddev", "export-db")
                    .withWorkDirectory(WslAware.toHostPath("/home/user/other", base));
            when(distribution.patchCommandLine(eq(command), isNull(), any(WSLCommandLineOptions.class)))
                    .thenReturn(command);

            assertThat(WslAware.patchCommandLine(command)).isSameAs(command);
            verify(distribution).patchCommandLine(eq(command), isNull(), any(WSLCommandLineOptions.class));
            assertThat(WslAware.toCommandPath("C:/backups/db.sql", selected)).isEqualTo("/mnt/c/backups/db.sql");
            assertThat(WslAware.toHostPath("/home/user/other", "/home/user/current")).isEqualTo("/home/user/other");
            assertThat(WslAware.toHostPath(selected, base)).isEqualTo(selected);
            assertThat(WslAware.toHostPath("/home/user/current", base)).isEqualTo(base);
            assertThat(WslAware.toHostPath("/mnt/c/project", base))
                    .isEqualTo("\\\\wsl.localhost\\Ubuntu\\mnt\\c\\project");
            assertThat(WslAware.toHostPath(null, base)).isNull();
        }
    }
}
