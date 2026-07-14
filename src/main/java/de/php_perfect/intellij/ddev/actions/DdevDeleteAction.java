package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevDeleteAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final Description description = DdevStateManager.getInstance(project).getState().getDescription();
        final String projectName = description != null && description.getName() != null
                ? description.getName()
                : project.getName();
        final boolean confirmed = MessageDialogBuilder.yesNo(
                DdevIntegrationBundle.message("toolWindow.projects.delete.confirm.title"),
                DdevIntegrationBundle.message("toolWindow.projects.delete.confirm.message", projectName)
        ).ask(project);

        if (confirmed) {
            DdevRunner.getInstance().delete(project);
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
