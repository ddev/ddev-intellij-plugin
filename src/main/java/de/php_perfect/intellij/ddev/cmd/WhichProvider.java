package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.wsl.WSLDistribution;
import com.intellij.execution.wsl.WslPath;
import com.intellij.openapi.util.SystemInfo;

final class WhichProvider {
    public static String getWhichCommand(String workingDirectory) {
        return getWhichCommand(workingDirectory, SystemInfo.isWindows);
    }

    static String getWhichCommand(String workingDirectory, boolean windows) {
        if (!windows) {
            return "which";
        }

        WSLDistribution distribution = WslPath.getDistributionByWindowsUncPath(workingDirectory);

        if (distribution != null) {
            return "which";
        }

        return "where";
    }

    private WhichProvider() {
    }
}
