package de.php_perfect.intellij.ddev.settings;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.dbmanager.DatabaseManager;
import de.php_perfect.intellij.ddev.docker.DockerProvider;
import de.php_perfect.intellij.ddev.expose.ExposeCommandsDialog;
import de.php_perfect.intellij.ddev.util.FeatureRequiredPlugins;
import de.php_perfect.intellij.ddev.util.PluginChecker;
import de.php_perfect.intellij.ddev.toolwindow.DdevProjectNameFormatter;
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
    private final @NotNull JBCheckBox createSnapshotOnStop = new JBCheckBox(DdevIntegrationBundle.message("settings.createSnapshotOnStop"));
    private final @NotNull JBCheckBox omitSnapshotOnDelete = new JBCheckBox(DdevIntegrationBundle.message("settings.omitSnapshotOnDelete"));
    private final @NotNull JBCheckBox deleteDdevFolderOnDelete = new JBCheckBox(DdevIntegrationBundle.message("settings.deleteDdevFolderOnDelete"));
    private final @NotNull JBCheckBox automaticallyInstallCms = new JBCheckBox(
            DdevIntegrationBundle.message("settings.automaticallyInstallCms"));
    private final @NotNull ComboBox<String> wordpressTablePrefixImportPolicy = new ComboBox<>(
            new String[]{"Ask", "Always", "Never"});
    private final @NotNull ComboBox<String> wordpressUrlImportPolicy = new ComboBox<>(
            new String[]{"Ask", "Always", "Never"});
    private final @NotNull TextFieldWithBrowseButton ddevBinary = new TextFieldWithBrowseButton();
    private final @NotNull ComboBox<String> projectNameFormat = new ComboBox<>(new String[]{
            DdevProjectNameFormatter.DEFAULT,
            DdevProjectNameFormatter.SPACES,
            DdevProjectNameFormatter.SENTENCE_CASE,
            DdevProjectNameFormatter.TITLE_CASE
    });
    private final @NotNull JBCheckBox expandServicesInProjectsToolWindow = new JBCheckBox(
            DdevIntegrationBundle.message("settings.projects.expandServices"));
    private final @NotNull JBCheckBox automaticallyStartProject = new JBCheckBox(
            DdevIntegrationBundle.message("settings.projects.automaticallyStart"));
    private final @NotNull JBTextField defaultShareProvider = new JBTextField();
    private final @NotNull ComboBox<DockerProvider> dockerProvider = new ComboBox<>(
            DockerProvider.available().toArray(new DockerProvider[0]));
    private final @NotNull JBTextField colimaArguments = new JBTextField();
    private final @NotNull ComboBox<String> databaseManager = new ComboBox<>(databaseManagerOptions());

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

        final JPanel snapshotPanel = new JPanel();
        snapshotPanel.setBorder(IdeBorderFactory.createTitledBorder(DdevIntegrationBundle.message("settings.snapshots"), true));
        snapshotPanel.setLayout(new BoxLayout(snapshotPanel, BoxLayout.Y_AXIS));
        snapshotPanel.add(this.createSnapshotOnStop);
        snapshotPanel.add(Box.createVerticalStrut(5));
        snapshotPanel.add(this.omitSnapshotOnDelete);
        snapshotPanel.add(Box.createVerticalStrut(5));
        snapshotPanel.add(this.deleteDdevFolderOnDelete);

        final JPanel projectsPanel = new JPanel();
        projectsPanel.setBorder(IdeBorderFactory.createTitledBorder(
                DdevIntegrationBundle.message("settings.projects"), true));
        projectsPanel.setLayout(new BoxLayout(projectsPanel, BoxLayout.Y_AXIS));
        projectsPanel.add(new JBLabel(DdevIntegrationBundle.message("settings.projects.nameFormat")));
        projectsPanel.add(this.projectNameFormat);
        projectsPanel.add(Box.createVerticalStrut(5));
        projectsPanel.add(this.expandServicesInProjectsToolWindow);
        projectsPanel.add(Box.createVerticalStrut(5));
        projectsPanel.add(this.automaticallyInstallCms);
        projectsPanel.add(Box.createVerticalStrut(5));
        projectsPanel.add(this.automaticallyStartProject);
        projectsPanel.add(comment(DdevIntegrationBundle.message("settings.projects.automaticallyStart.description")));

        this.defaultShareProvider.getEmptyText().setText(DdevIntegrationBundle.message("settings.share.defaultProvider.empty"));
        final JPanel sharePanel = titledPanel(DdevIntegrationBundle.message("settings.share"));
        sharePanel.add(new JBLabel(DdevIntegrationBundle.message("settings.share.defaultProvider")));
        sharePanel.add(this.defaultShareProvider);
        sharePanel.add(comment(DdevIntegrationBundle.message("settings.share.defaultProvider.description")));

        final JPanel dockerPanel = titledPanel(DdevIntegrationBundle.message("settings.docker"));
        dockerPanel.add(new JBLabel(DdevIntegrationBundle.message("settings.docker.provider")));
        dockerPanel.add(this.dockerProvider);
        dockerPanel.add(Box.createVerticalStrut(5));
        dockerPanel.add(new JBLabel(DdevIntegrationBundle.message("settings.docker.colimaArguments")));
        dockerPanel.add(this.colimaArguments);

        final JPanel databasePanel = titledPanel(DdevIntegrationBundle.message("settings.database"));
        databasePanel.add(new JBLabel(DdevIntegrationBundle.message("settings.database.manager")));
        databasePanel.add(this.databaseManager);

        final JPanel terminalPanel = titledPanel(DdevIntegrationBundle.message("settings.terminal"));
        final JButton configureExposedCommands = new JButton(DdevIntegrationBundle.message("settings.terminal.exposedCommands"));
        configureExposedCommands.addActionListener(event -> new ExposeCommandsDialog(project).show());
        terminalPanel.add(configureExposedCommands);
        terminalPanel.add(comment(DdevIntegrationBundle.message("settings.terminal.exposedCommands.description")));

        final JPanel wordpressPanel = new JPanel();
        wordpressPanel.setBorder(IdeBorderFactory.createTitledBorder(
                DdevIntegrationBundle.message("settings.wordpress"), true));
        wordpressPanel.setLayout(new BoxLayout(wordpressPanel, BoxLayout.Y_AXIS));
        wordpressPanel.add(new JBLabel(DdevIntegrationBundle.message("settings.wordpress.tablePrefixImportPolicy")));
        wordpressPanel.add(this.wordpressTablePrefixImportPolicy);
        wordpressPanel.add(Box.createVerticalStrut(5));
        wordpressPanel.add(new JBLabel(DdevIntegrationBundle.message("settings.wordpress.urlImportPolicy")));
        wordpressPanel.add(this.wordpressUrlImportPolicy);

        this.jPanel = FormBuilder.createFormBuilder()
                .addLabeledComponent(new JBLabel(DdevIntegrationBundle.message("settings.ddevBinary")), this.ddevBinary, 1, false)
                .addComponent(checkForUpdatesPanel, 1)
                .addComponent(watchDdevPanel, 1)
                .addComponent(panel, 1)
                .addComponent(snapshotPanel, 1)
                .addComponent(projectsPanel, 1)
                .addComponent(wordpressPanel, 1)
                .addComponent(sharePanel, 1)
                .addComponent(dockerPanel, 1)
                .addComponent(databasePanel, 1)
                .addComponent(terminalPanel, 1)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();
    }

    private static @NotNull String[] databaseManagerOptions() {
        final java.util.ArrayList<String> options = new java.util.ArrayList<>();
        options.add(DdevIntegrationBundle.message("settings.database.manager.ask"));
        DatabaseManager.available().forEach(manager -> options.add(manager.label()));
        return options.toArray(new String[0]);
    }

    private static @NotNull JPanel titledPanel(@NotNull String title) {
        final JPanel panel = new JPanel();
        panel.setBorder(IdeBorderFactory.createTitledBorder(title, true));
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        return panel;
    }

    private static @NotNull JComponent comment(@NotNull String text) {
        final JBLabel label = new JBLabel(text);
        label.setFont(JBUI.Fonts.smallFont());
        label.setForeground(UIManager.getColor("Component.infoForeground"));
        label.setAllowAutoWrapping(true);
        return label;
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

    public boolean getCreateSnapshotOnStop() {
        return this.createSnapshotOnStop.isSelected();
    }

    public void setCreateSnapshotOnStop(boolean newStatus) {
        this.createSnapshotOnStop.setSelected(newStatus);
    }

    public boolean getOmitSnapshotOnDelete() {
        return this.omitSnapshotOnDelete.isSelected();
    }

    public void setOmitSnapshotOnDelete(boolean newStatus) {
        this.omitSnapshotOnDelete.setSelected(newStatus);
    }

    public boolean getDeleteDdevFolderOnDelete() {
        return this.deleteDdevFolderOnDelete.isSelected();
    }

    public void setDeleteDdevFolderOnDelete(boolean newStatus) {
        this.deleteDdevFolderOnDelete.setSelected(newStatus);
    }

    public boolean getAutomaticallyInstallCms() {
        return this.automaticallyInstallCms.isSelected();
    }

    public void setAutomaticallyInstallCms(boolean automaticallyInstallCms) {
        this.automaticallyInstallCms.setSelected(automaticallyInstallCms);
    }

    public @NotNull String getWordpressTablePrefixImportPolicy() {
        final Object value = this.wordpressTablePrefixImportPolicy.getSelectedItem();
        return value instanceof String string ? string : "Ask";
    }

    public void setWordpressTablePrefixImportPolicy(@NotNull String policy) {
        this.wordpressTablePrefixImportPolicy.setSelectedItem(policy);
    }

    public @NotNull String getWordpressUrlImportPolicy() {
        final Object value = this.wordpressUrlImportPolicy.getSelectedItem();
        return value instanceof String string ? string : "Ask";
    }

    public void setWordpressUrlImportPolicy(@NotNull String policy) {
        this.wordpressUrlImportPolicy.setSelectedItem(policy);
    }

    public @NotNull String getProjectNameFormat() {
        final Object selected = this.projectNameFormat.getSelectedItem();
        return selected instanceof String value ? value : DdevProjectNameFormatter.DEFAULT;
    }

    public void setProjectNameFormat(@NotNull String format) {
        this.projectNameFormat.setSelectedItem(format);
    }

    public boolean getExpandServicesInProjectsToolWindow() {
        return this.expandServicesInProjectsToolWindow.isSelected();
    }

    public void setExpandServicesInProjectsToolWindow(boolean expanded) {
        this.expandServicesInProjectsToolWindow.setSelected(expanded);
    }

    public boolean getAutomaticallyStartProject() {
        return this.automaticallyStartProject.isSelected();
    }

    public void setAutomaticallyStartProject(boolean automaticallyStartProject) {
        this.automaticallyStartProject.setSelected(automaticallyStartProject);
    }

    public @NotNull String getDefaultShareProvider() {
        return this.defaultShareProvider.getText().trim();
    }

    public void setDefaultShareProvider(@NotNull String provider) {
        this.defaultShareProvider.setText(provider);
    }

    public @NotNull String getDockerProvider() {
        final Object selected = this.dockerProvider.getSelectedItem();
        return selected instanceof DockerProvider provider ? provider.value() : DockerProvider.AUTO_DETECT.value();
    }

    public void setDockerProvider(@NotNull String provider) {
        this.dockerProvider.setSelectedItem(DockerProvider.fromValue(provider));
    }

    public @NotNull String getColimaArguments() {
        return this.colimaArguments.getText().trim();
    }

    public void setColimaArguments(@NotNull String arguments) {
        this.colimaArguments.setText(arguments);
    }

    /**
     * Returns the configured manager's label, or an empty string when the user is asked each time.
     */
    public @NotNull String getDatabaseManager() {
        final DatabaseManager manager = DatabaseManager.fromValue((String) this.databaseManager.getSelectedItem());
        return manager == null ? "" : manager.label();
    }

    public void setDatabaseManager(@NotNull String manager) {
        final DatabaseManager selected = DatabaseManager.fromValue(manager);
        this.databaseManager.setSelectedItem(selected == null
                ? DdevIntegrationBundle.message("settings.database.manager.ask") : selected.label());
    }

    public @NotNull String getDdevBinary() {
        return this.ddevBinary.getText();
    }

    public void setDdevBinary(@NotNull String ddevBinary) {
        this.ddevBinary.setText(ddevBinary);
    }
}
