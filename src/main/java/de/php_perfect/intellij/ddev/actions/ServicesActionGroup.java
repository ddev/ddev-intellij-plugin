package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManager;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ServicesActionGroup extends ActionGroup implements DumbAware {
    @Override
    public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {

        if (e == null || e.getProject() == null) {
            return new AnAction[0];
        }

        return ServiceActionManager.getInstance(e.getProject()).getServiceActions();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();

        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(this.isActive(project));
    }

    private boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
        final Description description = state.getDescription();

        if (description == null) {
            return false;
        }

        return description.getStatus() == Description.Status.RUNNING;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
