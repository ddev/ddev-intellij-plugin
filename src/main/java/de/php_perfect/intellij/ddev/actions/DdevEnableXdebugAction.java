package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;

public final class DdevEnableXdebugAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        DdevRunner.getInstance().enableXdebug(project);
    }
}
