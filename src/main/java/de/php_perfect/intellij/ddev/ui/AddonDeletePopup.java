package de.php_perfect.intellij.ddev.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.List;

/**
 * Custom popup for displaying and filtering installed DDEV add-ons for deletion.
 */
public class AddonDeletePopup extends AbstractAddonPopup {
    public AddonDeletePopup(@NotNull Project project, @NotNull List<DdevAddon> addons) {
        super(project, "Search installed add-ons...", addons, new Dimension(500, 300));
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Select a DDEV Add-on to Remove";
    }

    @Override
    protected @NotNull String getRefreshButtonTooltip() {
        return "Refresh the list of installed add-ons";
    }

    @Override
    protected void handleListSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && addonList.getSelectedValue() != null) {
            DdevAddon selectedAddon = addonList.getSelectedValue();
            popup.closeOk(null);

            int result = Messages.showYesNoDialog(
                    project,
                    "Are you sure you want to remove the add-on '" + selectedAddon.getName() + "'?",
                    "Confirm Add-on Removal",
                    Messages.getQuestionIcon()
            );

            if (result == Messages.YES) {
                DdevRunner.getInstance().deleteAddon(project, selectedAddon);
            }
        }
    }

    @Override
    protected List<DdevAddon> getRefreshedAddons() {
        return DdevRunner.getInstance().getInstalledAddons(project);
    }

    @Override
    protected void handleEmptyList(String searchText) {
        // If no add-ons match the search, show a message
        addonList.setEmptyText("No add-ons found matching '" + searchText + "'");
    }

    @Override
    protected @NotNull Icon getAddonIcon() {
        return AllIcons.Actions.GC;
    }

    @Override
    protected boolean shouldShowDescription() {
        // Skip the description as it's redundant (just says "Installed add-on: X")
        return false;
    }
}
