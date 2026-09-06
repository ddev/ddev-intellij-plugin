package de.php_perfect.intellij.ddev.cmd.wsl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.wsl.WSLCommandLineOptions;
import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.nio.file.Path;

final class WslAwareTest {
    private static final String WSL_PATH = "\\\\wsl$\\Ubuntu\\home\\user\\project";

    @Test
    void keepsCommandLineOutsideWsl() {
        final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "start").withWorkDirectory("/home/user/project");

        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(Mockito.anyString())).thenReturn(null);

            Assertions.assertSame(commandLine, WslAware.patchCommandLine(commandLine));
        }
    }

    @Test
    void patchesCommandLineInsideWsl() throws ExecutionException {
        final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "start").withWorkDirectory(WSL_PATH);
        final GeneralCommandLine patched = new GeneralCommandLine("wsl.exe", "ddev", "start");
        final WSLDistribution distribution = Mockito.mock(WSLDistribution.class);
        final ArgumentCaptor<WSLCommandLineOptions> options = ArgumentCaptor.forClass(WSLCommandLineOptions.class);
        Mockito.when(distribution.patchCommandLine(Mockito.same(commandLine), Mockito.isNull(), options.capture())).thenReturn(patched);

        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(Mockito.anyString())).thenReturn(distribution);

            Assertions.assertSame(patched, WslAware.patchCommandLine(commandLine, true));
        }

        Assertions.assertTrue(options.getValue().isExecuteCommandInLoginShell());
    }

    @Test
    void keepsCommandLineWhenPatchingFails() throws ExecutionException {
        final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "start").withWorkDirectory(WSL_PATH);
        final WSLDistribution distribution = Mockito.mock(WSLDistribution.class);
        Mockito.when(distribution.patchCommandLine(Mockito.same(commandLine), Mockito.isNull(), Mockito.any(WSLCommandLineOptions.class)))
                .thenThrow(new ExecutionException("WSL unavailable"));

        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(Mockito.anyString())).thenReturn(distribution);

            Assertions.assertSame(commandLine, WslAware.patchCommandLine(commandLine));
        }
    }

    @Test
    void leavesHostCommandsWithoutAWorkingDirectoryAlone() {
        final GeneralCommandLine commandLine = new GeneralCommandLine("brew", "upgrade", "ddev/ddev/ddev");

        Assertions.assertSame(commandLine, WslAware.patchCommandLine(commandLine));
        Assertions.assertNull(commandLine.getWorkDirectory());
    }

    @Test
    void convertsSelectedProjectAndFilePathsUsingTheProjectsDistribution() throws ExecutionException {
        final String base = "\\\\wsl.localhost\\Ubuntu\\home\\user\\current";
        final String selected = "\\\\wsl.localhost\\Ubuntu\\home\\user\\other";
        final WSLDistribution distribution = Mockito.mock(WSLDistribution.class);

        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(base)).thenReturn(distribution);
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(selected)).thenReturn(distribution);
            wslPath.when(() -> WslPath.parseWindowsUncPath(base))
                    .thenReturn(new WslPath("\\\\wsl.localhost\\", "Ubuntu", "/home/user/current"));
            Mockito.when(distribution.getWslPath(Path.of("C:/backups/db.sql"))).thenReturn("/mnt/c/backups/db.sql");
            final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "export-db")
                    .withWorkDirectory(WslAware.toHostPath("/home/user/other", base));
            Mockito.when(distribution.patchCommandLine(Mockito.eq(commandLine), Mockito.isNull(), Mockito.any(WSLCommandLineOptions.class)))
                    .thenReturn(commandLine);

            Assertions.assertSame(commandLine, WslAware.patchCommandLine(commandLine));
            Mockito.verify(distribution).patchCommandLine(Mockito.eq(commandLine), Mockito.isNull(), Mockito.any(WSLCommandLineOptions.class));
            Assertions.assertEquals("/mnt/c/backups/db.sql", WslAware.toCommandPath("C:/backups/db.sql", selected));
            Assertions.assertEquals("/home/user/other", WslAware.toHostPath("/home/user/other", "/home/user/current"));
            Assertions.assertEquals(selected, WslAware.toHostPath(selected, base));
            Assertions.assertEquals(base, WslAware.toHostPath("/home/user/current", base));
            Assertions.assertEquals("\\\\wsl.localhost\\Ubuntu\\mnt\\c\\project", WslAware.toHostPath("/mnt/c/project", base));
            Assertions.assertNull(WslAware.toHostPath(null, base));
        }
    }
}
