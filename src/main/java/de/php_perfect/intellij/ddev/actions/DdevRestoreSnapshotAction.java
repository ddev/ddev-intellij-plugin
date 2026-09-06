package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.jetbrains.annotations.NotNull;

public final class DdevRestoreSnapshotAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        final String binary = DdevStateManager.getInstance(project).getState().getDdevBinary();

        if (binary != null) {
            SnapshotChooser.restore(project, binary, null);
        }
    }
}
