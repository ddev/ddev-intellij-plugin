package de.php_perfect.intellij.ddev.settings;

import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

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

        final JPanel panel = new JPanel();
        panel.setBorder(IdeBorderFactory.createTitledBorder(DdevIntegrationBundle.message("settings.automaticConfiguration"), true));
        panel.setLayout(new GridBagLayout());
        final GridBagConstraints gc = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, JBUI.emptyInsets(), 0, 0);
        panel.add(this.autoConfigureDataSource, gc);
        panel.add(this.autoConfigurePhpInterpreter, gc);
        panel.add(this.autoConfigureNodeJsInterpreter, gc);

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
