package de.php_perfect.intellij.ddev.service_actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.DescriptionChangedListener;
import de.php_perfect.intellij.ddev.cmd.Description;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ServiceActionChangedListener implements DescriptionChangedListener {
    private final @NotNull Project project;

    public ServiceActionChangedListener(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void onDescriptionChanged(@Nullable Description description) {
        ServiceActionManager.getInstance(this.project).updateActionsByDescription(description);
    }
}
