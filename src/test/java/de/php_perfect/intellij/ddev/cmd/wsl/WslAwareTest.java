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
}
