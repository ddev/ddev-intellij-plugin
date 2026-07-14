package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevRenameProjectAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
        final Description description = state.getDescription();
        final String currentName = description != null ? description.getName() : null;

        final String newName = Messages.showInputDialog(
                project,
                DdevIntegrationBundle.message("renameProject.message"),
                DdevIntegrationBundle.message("renameProject.title"),
                null,
                currentName,
                null
        );

        if (newName != null && !newName.isBlank() && !newName.equals(currentName)) {
            DdevRunner.getInstance().renameProject(project, null, currentName, newName.trim(), null);
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
