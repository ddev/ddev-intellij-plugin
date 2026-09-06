package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.dbmanager.DatabaseOpener;
import org.jetbrains.annotations.NotNull;

public final class DdevOpenDatabaseAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        DatabaseOpener.open(project, null, null);
    }
}
