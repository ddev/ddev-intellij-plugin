package de.php_perfect.intellij.ddev.version;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface VersionChecker {
    void checkDdevVersion();

    void checkDdevVersion(boolean confirmNewestVersion);

    static VersionChecker getInstance(@NotNull Project project) {
        return project.getService(VersionChecker.class);
    }
}
