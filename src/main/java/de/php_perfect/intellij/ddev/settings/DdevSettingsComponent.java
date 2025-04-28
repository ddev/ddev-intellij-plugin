package de.php_perfect.intellij.ddev.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.util.FeatureRequiredPlugins;
import de.php_perfect.intellij.ddev.util.PluginChecker;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.BoxLayout;
import java.awt.*;
import java.util.List;

public final class DdevSettingsComponent {
    private final @NotNull JPanel jPanel;
    private final @NotNull JBCheckBox checkForUpdatesCheckbox = new JBCheckBox(DdevIntegrationBundle.message("settings.checkForUpdates"));
    private final @NotNull JBCheckBox watchDdevCheckbox = new JBCheckBox(DdevIntegrationBundle.message("settings.watchDdev"));
    private final @NotNull JBCheckBox autoConfigureDataSource = new JBCheckBox(DdevIntegrationBundle.message("settings.automaticConfiguration.autoConfigureDataSource"));
    private final @NotNull JBCheckBox autoConfigurePhpInterpreter = new JBCheckBox(DdevIntegrationBundle.message("settings.automaticConfiguration.phpInterpreter"));
    private final @NotNull JBCheckBox autoConfigureNodeJsInterpreter = new JBCheckBox(DdevIntegrationBundle.message("settings.automaticConfiguration.nodeJsInterpreter"));
    private final @NotNull TextFieldWithBrowseButton ddevBinary = new TextFieldWithBrowseButton();

    public DdevSettingsComponent(Project project) {
        // Create panels with checkboxes and comments manually instead of using deprecated UI.PanelFactory
        JPanel checkForUpdatesPanel = new JPanel(new BorderLayout());
        checkForUpdatesPanel.add(this.checkForUpdatesCheckbox, BorderLayout.NORTH);
        JLabel checkForUpdatesComment = new JLabel(DdevIntegrationBundle.message("settings.checkForUpdates.description"));
        checkForUpdatesComment.setFont(JBUI.Fonts.smallFont());
        checkForUpdatesComment.setForeground(UIManager.getColor("Component.infoForeground"));
        checkForUpdatesComment.setBorder(JBUI.Borders.emptyLeft(24));
        checkForUpdatesPanel.add(checkForUpdatesComment, BorderLayout.CENTER);

        JPanel watchDdevPanel = new JPanel(new BorderLayout());
        watchDdevPanel.add(this.watchDdevCheckbox, BorderLayout.NORTH);
        JLabel watchDdevComment = new JLabel(DdevIntegrationBundle.message("settings.watchDdev.description"));
        watchDdevComment.setFont(JBUI.Fonts.smallFont());
        watchDdevComment.setForeground(UIManager.getColor("Component.infoForeground"));
        watchDdevComment.setBorder(JBUI.Borders.emptyLeft(24));
        watchDdevPanel.add(watchDdevComment, BorderLayout.CENTER);

        // Set up warning indicators and tooltips
        @NotNull JPanel autoConfigureDataSourcePanel = new JPanel(new BorderLayout());
        @NotNull JLabel dataSourceWarningLabel = new JLabel(AllIcons.General.Warning);
        @NotNull JLabel dataSourceWarningText = new JLabel();
        setupWarningIndicator(autoConfigureDataSourcePanel, this.autoConfigureDataSource, dataSourceWarningLabel,
                dataSourceWarningText, FeatureRequiredPlugins.DATABASE);

        @NotNull JPanel autoConfigurePhpInterpreterPanel = new JPanel(new BorderLayout());
        @NotNull JLabel phpWarningLabel = new JLabel(AllIcons.General.Warning);
        @NotNull JLabel phpWarningText = new JLabel();
        setupWarningIndicator(autoConfigurePhpInterpreterPanel, this.autoConfigurePhpInterpreter, phpWarningLabel,
                phpWarningText, FeatureRequiredPlugins.PHP_INTERPRETER);

        @NotNull JPanel autoConfigureNodeJsInterpreterPanel = new JPanel(new BorderLayout());
        @NotNull JLabel nodeWarningLabel = new JLabel(AllIcons.General.Warning);
        @NotNull JLabel nodeWarningText = new JLabel();
        setupWarningIndicator(autoConfigureNodeJsInterpreterPanel, this.autoConfigureNodeJsInterpreter, nodeWarningLabel,
                nodeWarningText, FeatureRequiredPlugins.NODE_INTERPRETER);

        final JPanel panel = new JPanel();
        panel.setBorder(IdeBorderFactory.createTitledBorder(DdevIntegrationBundle.message("settings.automaticConfiguration"), true));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add feature panels with some vertical spacing
        panel.add(autoConfigureDataSourcePanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(autoConfigurePhpInterpreterPanel);
        panel.add(Box.createVerticalStrut(5));
        panel.add(autoConfigureNodeJsInterpreterPanel);

        this.ddevBinary.addBrowseFolderListener(
                project,
                new FileChooserDescriptor(true, false, false, false, false, false)
                        .withTitle(DdevIntegrationBundle.message("settings.chooseBinary.title"))
                        .withDescription("")
        );

        this.jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(DdevIntegrationBundle.message("settings.ddevBinary")), this.ddevBinary, 1, false)
                .addComponent(checkForUpdatesPanel, 1)
                .addComponent(watchDdevPanel, 1)
                .addComponent(panel, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    /**
     * Sets up a warning indicator for a feature that requires plugins.
     *
     * @param panel The panel to add the components to
     * @param checkbox The checkbox for the feature
     * @param warningLabel The warning icon to show if plugins are missing
     * @param warningText The label to display the warning text
     * @param requiredPlugins The list of required plugin IDs
     */
    private void setupWarningIndicator(@NotNull JPanel panel, @NotNull JBCheckBox checkbox, @NotNull JLabel warningLabel,
                                       @NotNull JLabel warningText, @NotNull List<String> requiredPlugins) {
        // Set up panel with vertical layout
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        // Add checkbox to panel
        JPanel checkboxPanel = new JPanel(new BorderLayout());
        checkboxPanel.add(checkbox, BorderLayout.WEST);
        panel.add(checkboxPanel);

        // Check if any required plugins are missing
        List<String> missingPlugins = PluginChecker.getMissingPlugins(requiredPlugins);

        if (missingPlugins.isEmpty()) {
            // No missing plugins, hide warning
            warningLabel.setVisible(false);
            warningText.setVisible(false);
        } else {
            // Create warning message
            String warningMessage = DdevIntegrationBundle.message("settings.warning.missingPlugins", String.join(", ", missingPlugins));

            // Set up warning text
            warningText.setText(" " + warningMessage);
            warningText.setForeground(UIUtil.getErrorForeground());

            // Create warning panel with left alignment and indentation
            JPanel warningPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            warningPanel.setBorder(JBUI.Borders.emptyLeft(24));
            warningPanel.add(warningLabel);
            warningPanel.add(warningText);

            // Add warning panel below the checkbox
            panel.add(warningPanel);
        }
    }

    public @NotNull JPanel getPanel() {
        return this.jPanel;
    }

    public @NotNull JComponent getPreferredFocusedComponent() {
        return this.checkForUpdatesCheckbox;
    }

    public boolean getCheckForUpdatedStatus() {
        return this.checkForUpdatesCheckbox.isSelected();
    }

    public void setCheckForUpdatesStatus(boolean newStatus) {
        this.checkForUpdatesCheckbox.setSelected(newStatus);
    }

    public boolean getWatchDdevCheckboxStatus() {
        return this.watchDdevCheckbox.isSelected();
    }

    public void setAutoConfigureDataSource(boolean newStatus) {
        this.autoConfigureDataSource.setSelected(newStatus);
    }

    public boolean getAutoConfigureDataSource() {
        return this.autoConfigureDataSource.isSelected();
    }

    public void setAutoConfigurePhpInterpreter(boolean newStatus) {
        this.autoConfigurePhpInterpreter.setSelected(newStatus);
    }

    public boolean getAutoConfigurePhpInterpreter() {
        return this.autoConfigurePhpInterpreter.isSelected();
    }

    public void setAutoConfigureNodeJsInterpreter(boolean newStatus) {
        this.autoConfigureNodeJsInterpreter.setSelected(newStatus);
    }

    public boolean getAutoConfigureNodeJsInterpreter() {
        return this.autoConfigureNodeJsInterpreter.isSelected();
    }

    public void setWatchDdevCheckboxStatus(boolean newStatus) {
        this.watchDdevCheckbox.setSelected(newStatus);
    }

    public @NotNull String getDdevBinary() {
        return this.ddevBinary.getText();
    }

    public void setDdevBinary(@NotNull String ddevBinary) {
        this.ddevBinary.setText(ddevBinary);
    }
}
