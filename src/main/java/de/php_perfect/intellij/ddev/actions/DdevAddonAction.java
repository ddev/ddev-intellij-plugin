package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for DDEV add-on related actions.
 */
public abstract class DdevAddonAction extends DdevAwareAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        super.update(e);

        Project project = e.getProject();

        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        e.getPresentation().setEnabled(isActive(project));
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
    
        if (!state.isAvailable() || !state.isConfigured()) {
            return false;
        }
    
        if (state.getDescription() == null) {
            return false;
        }
    
        return state.getDescription().getStatus() == de.php_perfect.intellij.ddev.cmd.Description.Status.RUNNING;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
