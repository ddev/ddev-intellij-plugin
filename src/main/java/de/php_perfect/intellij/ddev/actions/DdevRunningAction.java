package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

/**
 * Base class for actions that require the DDEV project of the current IDE project to be running.
 */
abstract class DdevRunningAction extends DdevRunAction {
    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable()) {
            return false;
        }

        final Description description = state.getDescription();

        if (description == null) {
            return false;
        }

        return description.getStatus() == Description.Status.RUNNING;
    }
}
