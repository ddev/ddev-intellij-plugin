package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.util.SystemInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

final class WhichProviderTest {
    private static final String WSL_PATH = "\\\\wsl$\\Ubuntu\\home\\user\\project";

    @Test
    void usesWhichOutsideWindows() {
        Assertions.assertEquals("which", WhichProvider.getWhichCommand("/home/user/project", false));
    }

    @Test
    void usesWhereOnWindows() {
        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath("C:\\project")).thenReturn(null);

            Assertions.assertEquals("where", WhichProvider.getWhichCommand("C:\\project", true));
        }
    }

    @Test
    void usesWhichInsideWsl() {
        try (MockedStatic<WslPath> wslPath = Mockito.mockStatic(WslPath.class)) {
            wslPath.when(() -> WslPath.getDistributionByWindowsUncPath(WSL_PATH)).thenReturn(Mockito.mock(WSLDistribution.class));

            Assertions.assertEquals("which", WhichProvider.getWhichCommand(WSL_PATH, true));
        }
    }

    @Test
    void followsCurrentPlatform() {
        Assertions.assertEquals(SystemInfo.isWindows ? "where" : "which", WhichProvider.getWhichCommand(System.getProperty("java.io.tmpdir")));
    }
}
