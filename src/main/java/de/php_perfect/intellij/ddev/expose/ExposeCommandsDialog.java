package de.php_perfect.intellij.ddev.expose;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.JBUI;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.settings.DdevApplicationSettings;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Chooses the container commands exposed in the terminal, for this project or for all DDEV projects.
 */
public final class ExposeCommandsDialog extends DialogWrapper {
    private final @NotNull Project project;
    private final @NotNull ComboBox<String> scope = new ComboBox<>(new String[]{
            DdevIntegrationBundle.message("expose.scope.project"),
            DdevIntegrationBundle.message("expose.scope.global")
    });
    private final @NotNull CheckBoxList<String> commands = new CheckBoxList<>();
    private final @NotNull JBTextField custom = new JBTextField();
    private final @NotNull List<String> projectEntries;
    private final @NotNull List<String> globalEntries;
    private int shownScope = 0;

    public ExposeCommandsDialog(@NotNull Project project) {
        super(project);
        this.project = project;
        this.projectEntries = new ArrayList<>(DdevSettingsState.getInstance(project).exposedCommands);
        this.globalEntries = new ArrayList<>(DdevApplicationSettings.getInstance().exposedCommands);
        this.setTitle(DdevIntegrationBundle.message("expose.title"));
        this.init();
        this.showScope(0);
        this.scope.addActionListener(event -> {
            this.entries(this.shownScope).clear();
            this.entries(this.shownScope).addAll(this.checked());
            this.showScope(this.scope.getSelectedIndex());
        });
    }

    @Override
    protected @NotNull JComponent createCenterPanel() {
        final JButton add = new JButton(DdevIntegrationBundle.message("expose.add"));
        add.addActionListener(event -> this.addCustom());
        this.custom.getEmptyText().setText(DdevIntegrationBundle.message("expose.custom.empty"));
        final JPanel customRow = new JPanel(new BorderLayout(JBUI.scale(4), 0));
        customRow.add(this.custom, BorderLayout.CENTER);
        customRow.add(add, BorderLayout.EAST);

        final JScrollPane list = ScrollPaneFactory.createScrollPane(this.commands);
        list.setPreferredSize(JBUI.size(420, 320));

        return FormBuilder.createFormBuilder()
                .addLabeledComponent(DdevIntegrationBundle.message("expose.scope"), this.scope)
                .addComponent(new JBLabel(DdevIntegrationBundle.message("expose.description")))
                .addComponentFillVertically(list, 4)
                .addComponent(customRow)
                .getPanel();
    }

    @Override
    protected void doOKAction() {
        this.entries(this.shownScope).clear();
        this.entries(this.shownScope).addAll(this.checked());
        DdevSettingsState.getInstance(this.project).exposedCommands = new ArrayList<>(this.projectEntries);
        DdevApplicationSettings.getInstance().exposedCommands = new ArrayList<>(this.globalEntries);
        super.doOKAction();
    }

    private void addCustom() {
        final String entry = this.custom.getText().trim();
        if (ExposedCommands.parse(entry) == null) {
            this.setErrorInfoAll(List.of(new ValidationInfo(DdevIntegrationBundle.message("expose.custom.invalid"), this.custom)));
            return;
        }
        this.setErrorInfoAll(List.of());
        final List<String> checked = this.checked();
        checked.add(entry);
        this.fill(checked);
        this.custom.setText("");
    }

    private void showScope(int index) {
        this.shownScope = index;
        this.fill(this.entries(index));
    }

    private void fill(@NotNull List<String> checked) {
        final Set<String> offered = new LinkedHashSet<>(ExposedCommands.SUGGESTED);
        offered.addAll(this.vendorBinaries());
        offered.addAll(checked);
        this.commands.clear();
        for (String entry : offered.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList()) {
            this.commands.addItem(entry, entry, checked.contains(entry));
        }
    }

    private @NotNull List<String> checked() {
        final List<String> checked = new ArrayList<>();
        for (int i = 0; i < this.commands.getItemsCount(); i++) {
            final String entry = this.commands.getItemAt(i);
            if (entry != null && this.commands.isItemSelected(i)) {
                checked.add(entry);
            }
        }
        return checked;
    }

    private @NotNull List<String> entries(int scopeIndex) {
        return scopeIndex == 0 ? this.projectEntries : this.globalEntries;
    }

    /**
     * The project's Composer binaries, which are on the web container's PATH.
     */
    private @NotNull List<String> vendorBinaries() {
        final String root = DdevProjectRoot.of(this.project);
        final List<String> binaries = new ArrayList<>();
        if (root == null || !Files.isDirectory(Path.of(root, "vendor", "bin"))) {
            return binaries;
        }
        try (DirectoryStream<Path> files = Files.newDirectoryStream(Path.of(root, "vendor", "bin"))) {
            for (Path file : files) {
                final String name = file.getFileName().toString();
                if (!name.startsWith(".") && !name.endsWith(".bat") && ExposedCommands.parse(name) != null) {
                    binaries.add(name);
                }
            }
        } catch (IOException ignored) {
            // The suggestions remain available without the project's binaries.
        }
        return binaries;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return this.commands;
    }
}
