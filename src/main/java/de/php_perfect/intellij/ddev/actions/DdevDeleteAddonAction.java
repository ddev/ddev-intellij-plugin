package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.ui.AddonDeletePopup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Action to remove an add-on from a DDEV project.
 */
public final class DdevDeleteAddonAction extends DdevAddonAction {

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        List<DdevAddon> installedAddons = DdevRunner.getInstance().getInstalledAddons(project);

        if (installedAddons.isEmpty()) {
            Messages.showInfoMessage(
                    project,
                    "No add-ons installed that can be removed.",
                    "DDEV Add-Ons"
            );
            return;
        }

        // Show the custom add-on delete popup
        new AddonDeletePopup(project, installedAddons).show();
    }
}
