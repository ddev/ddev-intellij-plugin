package de.php_perfect.intellij.ddev.service_actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ServiceActionManager {

    AnAction @NotNull [] getServiceActions();

    void updateActionsByDescription(@Nullable Description description);

    static ServiceActionManager getInstance(@NotNull Project project) {
        return project.getService(ServiceActionManager.class);
    }
}
