package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.ShareManager;
import org.jetbrains.annotations.NotNull;

public final class DdevShareAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        DdevRunner.getInstance().share(project);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return super.isActive(project) && !ShareManager.getInstance(project).isSharing();
    }
}
