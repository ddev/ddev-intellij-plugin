package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.ui.AddonListPopup;
import org.jetbrains.annotations.NotNull;

/**
 * Action to add an add-on to a DDEV project.
 */
public final class DdevAddAddonAction extends DdevAddonAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        new AddonListPopup(project).show();
    }
}
