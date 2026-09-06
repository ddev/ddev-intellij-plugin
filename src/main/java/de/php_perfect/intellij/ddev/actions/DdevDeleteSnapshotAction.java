package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevDeleteSnapshotAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();

        if (binary != null) {
            SnapshotChooser.delete(project, binary, null);
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
