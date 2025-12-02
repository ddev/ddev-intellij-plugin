package de.php_perfect.intellij.ddev.ui;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import de.php_perfect.intellij.ddev.cmd.DdevAddon;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for DDEV addon popups with common functionality.
 */
public abstract class AbstractAddonPopup {
    protected static final Logger LOG = Logger.getInstance(AbstractAddonPopup.class);

    protected final Project project;
    protected final List<DdevAddon> allAddons = new ArrayList<>();
    protected final JBList<DdevAddon> addonList;
    protected final DefaultListModel<DdevAddon> listModel;
    protected final JBTextField searchField;
    protected final JPanel panel;
    protected final JButton refreshButton;
    protected JBPopup popup;
    protected Timer refreshTimer;

    /**
     * Creates a new addon popup.
     *
     * @param project The project context
     * @param searchPlaceholder Text to show in the search field when empty
     * @param initialAddons Initial list of addons to display (can be empty)
     * @param dimensions Initial dimensions for the popup
     */
    protected AbstractAddonPopup(@NotNull Project project, @NotNull String searchPlaceholder,
                               @Nullable List<DdevAddon> initialAddons, @NotNull Dimension dimensions) {
        this.project = project;

        // Initialize components
        this.listModel = new DefaultListModel<>();
        this.addonList = new JBList<>(listModel);
        this.searchField = new JBTextField();
        this.refreshButton = new JButton("Refresh", AllIcons.Actions.Refresh);
        this.panel = new JPanel(new BorderLayout());

        // Add initial addons if provided
        if (initialAddons != null) {
            // Sort add-ons by name
            this.allAddons.addAll(initialAddons.stream()
                    .sorted((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()))
                    .toList());
        }

        // Initialize UI
        initializeUI(searchPlaceholder, dimensions);

        // Update the list initially
        updateList("");
    }

    /**
     * Sets up the UI components.
     *
     * @param searchPlaceholder Text to show in search field when empty
     * @param dimensions Initial dimensions for the popup
     */
    protected void initializeUI(@NotNull String searchPlaceholder, @NotNull Dimension dimensions) {
        // Set up the search field
        searchField.getEmptyText().setText(searchPlaceholder);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateList(searchField.getText());
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateList(searchField.getText());
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateList(searchField.getText());
            }
        });

        // Set up the add-on list
        addonList.setCellRenderer(createListCellRenderer());
        addonList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addonList.addListSelectionListener(this::handleListSelection);

        // Set up the refresh button
        refreshButton.setToolTipText(getRefreshButtonTooltip());
        refreshButton.addActionListener(e -> handleRefreshAction());

        // Create the panel
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(refreshButton, BorderLayout.EAST);
        searchPanel.setBorder(JBUI.Borders.empty(5));

        panel.add(searchPanel, BorderLayout.NORTH);
        panel.add(new JBScrollPane(addonList), BorderLayout.CENTER);
        panel.setPreferredSize(dimensions);
    }

    /**
     * Updates the list based on the search text.
     *
     * @param searchText The text to filter by
     */
    protected void updateList(String searchText) {
        listModel.clear();

        List<DdevAddon> filteredAddons = filterAddons(searchText);
        for (DdevAddon addon : filteredAddons) {
            listModel.addElement(addon);
        }

        if (filteredAddons.isEmpty()) {
            handleEmptyList(searchText);
        }
    }

    /**
     * Filters addons based on search text.
     *
     * @param searchText The text to filter by
     * @return Filtered list of addons
     */
    protected List<DdevAddon> filterAddons(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            return new ArrayList<>(allAddons);
        }

        String lowerCaseSearchText = searchText.toLowerCase();

        return allAddons.stream()
                .filter(addon -> {
                    // Check if the add-on name contains the search text
                    if (addon.getName().toLowerCase().contains(lowerCaseSearchText)) {
                        return true;
                    }

                    // Check if the description contains the search text
                    if (addon.getDescription().toLowerCase().contains(lowerCaseSearchText)) {
                        return true;
                    }

                    // Check if the vendor name contains the search text
                    if (addon.getVendorName() != null &&
                        addon.getVendorName().toLowerCase().contains(lowerCaseSearchText)) {
                        return true;
                    }

                    // Check if the type contains the search text
                    return addon.getType() != null &&
                            addon.getType().toLowerCase().contains(lowerCaseSearchText);
                })
                .toList();
    }

    /**
     * Shows the popup.
     */
    public void show() {
        ComponentPopupBuilder builder = JBPopupFactory.getInstance()
                .createComponentPopupBuilder(panel, searchField)
                .setTitle(getPopupTitle())
                .setResizable(true)
                .setMovable(true)
                .setRequestFocus(true);

        popup = builder.createPopup();

        // Add listeners
        popup.addListener(createPopupListener());

        // Show the popup
        popup.showCenteredInCurrentWindow(project);
    }

    /**
     * Creates a cell renderer for the addon list.
     *
     * @return The cell renderer
     */
    protected ListCellRenderer<DdevAddon> createListCellRenderer() {
        return new AddonListCellRenderer(getAddonIcon(), shouldShowDescription());
    }

    /**
     * Creates a popup listener.
     *
     * @return The popup listener
     */
    protected JBPopupListener createPopupListener() {
        return new JBPopupListener() {
            @Override
            public void onClosed(@NotNull LightweightWindowEvent event) {
                // Hook for cleanup actions
                handlePopupClosed();
            }
        };
    }

    // Abstract methods to be implemented by subclasses

    /**
     * Gets the title for the popup.
     *
     * @return The popup title
     */
    protected abstract @NotNull String getPopupTitle();

    /**
     * Gets the tooltip text for the refresh button.
     *
     * @return The tooltip text
     */
    protected abstract @NotNull String getRefreshButtonTooltip();

    /**
     * Handles the list selection event.
     *
     * @param e The list selection event
     */
    protected abstract void handleListSelection(ListSelectionEvent e);

    /**
     * Handles the refresh button action.
     * This is a template method that calls the specific implementation methods.
     */
    protected void handleRefreshAction() {
        // Show a loading indicator
        addonList.setPaintBusy(true);
        addonList.setEmptyText("Refreshing add-ons...");

        // Stop any existing timer
        stopRefreshTimer();

        // Create a new timer to refresh the add-ons
        refreshTimer = new Timer(getRefreshDelayMillis(), event -> {
            // Get the refreshed add-ons
            List<DdevAddon> refreshedAddons = getRefreshedAddons();

            // Sort add-ons by name
            List<DdevAddon> sortedAddons = refreshedAddons.stream()
                    .sorted((a1, a2) -> a1.getName().compareToIgnoreCase(a2.getName()))
                    .toList();

            // Update the allAddons list
            allAddons.clear();
            allAddons.addAll(sortedAddons);

            // Update the list
            updateList(searchField.getText());

            // Stop the loading indicator
            addonList.setPaintBusy(false);

            // Log the refresh
            LOG.debug("Add-ons refreshed successfully: " + refreshedAddons.size() + " add-ons found");
        });

        refreshTimer.setRepeats(false);
        refreshTimer.start();
    }

    /**
     * Gets the delay in milliseconds to wait before refreshing the add-ons.
     * Default is 1000ms (1 second).
     *
     * @return The delay in milliseconds
     */
    protected int getRefreshDelayMillis() {
        return 1000;
    }

    /**
     * Gets the refreshed add-ons.
     * This method should be implemented by subclasses to provide the specific add-ons to display.
     *
     * @return The refreshed add-ons
     */
    protected abstract List<DdevAddon> getRefreshedAddons();

    /**
     * Stops the refresh timer if it's running.
     */
    protected void stopRefreshTimer() {
        if (refreshTimer != null && refreshTimer.isRunning()) {
            refreshTimer.stop();
        }
    }

    /**
     * Handles what to show when the filtered list is empty.
     *
     * @param searchText The current search text
     */
    protected abstract void handleEmptyList(String searchText);

    /**
     * Gets the icon to use for addons in the list.
     *
     * @return The icon
     */
    protected abstract @NotNull Icon getAddonIcon();

    /**
     * Determines whether to show the description in the list.
     *
     * @return true to show the description, false otherwise
     */
    protected abstract boolean shouldShowDescription();

    /**
     * Called when the popup is closed.
     * Default implementation stops the refresh timer.
     */
    protected void handlePopupClosed() {
        stopRefreshTimer();
    }

    /**
     * Cell renderer for addon items.
     */
    protected static class AddonListCellRenderer extends ColoredListCellRenderer<DdevAddon> {
        private final transient Icon addonIcon;
        private final boolean showDescription;

        /**
         * Creates a new addon list cell renderer.
         *
         * @param addonIcon The icon to use for addons
         * @param showDescription Whether to show the description
         */
        public AddonListCellRenderer(@NotNull Icon addonIcon, boolean showDescription) {
            this.addonIcon = addonIcon;
            this.showDescription = showDescription;
        }

        @Override
        protected void customizeCellRenderer(@NotNull JList<? extends DdevAddon> list, DdevAddon addon,
                                          int index, boolean selected, boolean hasFocus) {
            // Set the add-on name
            append(addon.getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            // Add vendor name as a comment if available
            if (addon.getVendorName() != null && !addon.getVendorName().isEmpty()) {
                append(" (" + addon.getVendorName() + ")", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // Add version if available
            if (addon.getVersion() != null) {
                append(" v" + addon.getVersion(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
            }

            // Add type if available
            if (addon.getType() != null) {
                SimpleTextAttributes typeAttributes = "official".equals(addon.getType())
                        ? new SimpleTextAttributes(SimpleTextAttributes.STYLE_BOLD, JBUI.CurrentTheme.Link.Foreground.ENABLED)
                        : SimpleTextAttributes.GRAY_ATTRIBUTES;
                append(" [" + addon.getType() + "]", typeAttributes);
            }

            // Add stars if available
            if (addon.getStars() > 0) {
                append(" ★" + addon.getStars(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }

            // Add description if needed
            if (showDescription && !addon.getDescription().isEmpty()) {
                append(" - " + addon.getDescription(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            }

            // Set icon
            setIcon(addonIcon);
        }
    }
}
