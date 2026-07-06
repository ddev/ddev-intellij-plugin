package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.ShareManager;
import org.jetbrains.annotations.NotNull;

public final class DdevShareStopAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        DdevRunner.getInstance().stopShare(project);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return ShareManager.getInstance(project).isSharing();
    }
}
