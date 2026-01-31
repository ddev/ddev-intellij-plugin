package de.php_perfect.intellij.ddev.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.addon.AddonCache;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Custom popup for displaying and filtering DDEV add-ons.
 */
public class AddonListPopup extends AbstractAddonPopup {
    private static final Logger LOG = Logger.getInstance(AddonListPopup.class);
    private final AtomicBoolean isLoading = new AtomicBoolean(false);
    private Timer loadingCheckTimer;

    /**
     * Creates a new AddonListPopup with an empty list that will be populated asynchronously.
     *
     * @param project The project to get add-ons for
     */
    public AddonListPopup(@NotNull Project project) {
        super(project, "Search add-ons...", null, new Dimension(600, 400));

        // Show loading indicator
        addonList.setPaintBusy(true);
        addonList.setEmptyText("Loading add-ons...");

        // Start loading add-ons in the background
        loadAddonsAsync();
    }

    /**
     * Loads add-ons asynchronously and updates the UI when they are available.
     */
    private void loadAddonsAsync() {
        if (isLoading.compareAndSet(false, true)) {
            // Trigger a refresh of the cache to make sure we get the latest data
            AddonCache.getInstance(project).refreshCacheAsync();

            // Create a timer to check for add-ons periodically
            loadingCheckTimer = new Timer(500, e -> {
                List<DdevAddon> availableAddons = getRefreshedAddons();

                if (!availableAddons.isEmpty()) {
                    // We have add-ons, stop the timer and update the UI
                    loadingCheckTimer.stop();

                    // Sort and update the list using the same logic as in handleRefreshAction
                    List<DdevAddon> sortedAddons = availableAddons.stream()
                            .sorted((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()))
                            .toList();

                    allAddons.clear();
                    allAddons.addAll(sortedAddons);
                    updateList(searchField.getText());

                    // Stop the loading indicator
                    addonList.setPaintBusy(false);

                    LOG.debug("Add-ons loaded successfully: " + availableAddons.size() + " add-ons found");
                }
            });

            loadingCheckTimer.setRepeats(true);
            loadingCheckTimer.start();
        }
    }

    @Override
    protected @NotNull String getPopupTitle() {
        return "Select a DDEV Add-on to Add";
    }

    @Override
    protected @NotNull String getRefreshButtonTooltip() {
        return "Refresh the list of available add-ons";
    }

    @Override
    protected void handleListSelection(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && addonList.getSelectedValue() != null) {
            DdevAddon selectedAddon = addonList.getSelectedValue();
            popup.closeOk(null);
            DdevRunner.getInstance().addAddon(project, selectedAddon);
        }
    }

    @Override
    protected void handleRefreshAction() {
        // Show a loading indicator
        addonList.setPaintBusy(true);
        addonList.setEmptyText("Refreshing add-ons...");
        isLoading.set(true);

        // Clear the current list
        allAddons.clear();
        updateList(searchField.getText());

        // Refresh the cache in the background
        AddonCache.getInstance(project).refreshCacheAsync();

        // Use the base class refresh logic
        super.handleRefreshAction();
    }

    @Override
    protected List<DdevAddon> getRefreshedAddons() {
        List<DdevAddon> availableAddons = DdevRunner.getInstance().getAvailableAddons(project);
        if (!availableAddons.isEmpty()) {
            isLoading.set(false);
        }
        return availableAddons;
    }

    @Override
    protected int getRefreshDelayMillis() {
        return 500; // Use a shorter delay for available add-ons
    }

    @Override
    protected void handleEmptyList(String searchText) {
        if (isLoading.get()) {
            // If we're still loading, show a loading message
            addonList.setEmptyText("Loading add-ons...");
        } else if (allAddons.isEmpty()) {
            // If there are no add-ons at all, show a message
            addonList.setEmptyText("No add-ons available");
        } else {
            // If no add-ons match the search, show a message
            addonList.setEmptyText("No add-ons found matching '" + searchText + "'");
        }
    }

    @Override
    protected @NotNull Icon getAddonIcon() {
        return AllIcons.Nodes.Plugin;
    }

    @Override
    protected boolean shouldShowDescription() {
        return true;
    }

    @Override
    protected void handlePopupClosed() {
        super.handlePopupClosed();
        if (loadingCheckTimer != null && loadingCheckTimer.isRunning()) {
            loadingCheckTimer.stop();
        }
    }
}
