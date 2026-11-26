package de.php_perfect.intellij.ddev.cmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface DdevRunner {

    void start(@NotNull Project project);

    void restart(@NotNull Project project);

    void stop(@NotNull Project project);

    void powerOff(@NotNull Project project);

    void delete(@NotNull Project project);

    void share(@NotNull Project project);

    void stopShare(@NotNull Project project);

    void config(@NotNull Project project);

    static DdevRunner getInstance() {
        return ApplicationManager.getApplication().getService(DdevRunner.class);
    }
}
