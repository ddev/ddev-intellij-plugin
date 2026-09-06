package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.ui.components.JBList;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.AddOn;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.InstalledAddOn;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Lets the user pick a DDEV add-on to install or remove. A {@code null} working directory
 * targets the IDE project; any other directory targets that DDEV project instead.
 */
public final class AddOnChooser {
    private AddOnChooser() {
    }

    public static void install(@NotNull Project project, @NotNull String binary, @Nullable String workingDirectory,
                               @Nullable Runnable afterCompletion) {
        new Task.Backgroundable(project, DdevIntegrationBundle.message("addOn.loadingAvailable"), true) {
            private @Nullable List<AddOn> addOns;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final Set<String> installedRepositories = Ddev.getInstance()
                            .listInstalledAddOns(binary, project, workingDirectory).stream()
                            .map(InstalledAddOn::getRepository)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toSet());
                    this.addOns = Ddev.getInstance().listAddOns(binary, project).stream()
                            .filter(addOn -> addOn.getTitle() != null)
                            .filter(addOn -> !installedRepositories.contains(addOn.getTitle()))
                            .toList();
                } catch (CommandFailedException exception) {
                    DdevNotifier.getInstance(project).notifyAddOnListFailed();
                }
            }

            @Override
            public void onSuccess() {
                if (this.addOns == null || this.addOns.isEmpty()) {
                    return;
                }

                final JBList<AddOn> list = new JBList<>(this.addOns);
                // Add-ons are identified by their GitHub repository, which describes them in detail.
                list.registerKeyboardAction(event -> {
                    final AddOn selected = list.getSelectedValue();
                    if (selected != null && selected.getTitle() != null) {
                        BrowserUtil.browse("https://github.com/" + selected.getTitle());
                    }
                }, KeyStroke.getKeyStroke(KeyEvent.VK_B, SystemInfo.isMac ? InputEvent.META_DOWN_MASK : InputEvent.CTRL_DOWN_MASK),
                        JComponent.WHEN_IN_FOCUSED_WINDOW);

                new PopupChooserBuilder<>(list)
                        .setTitle(DdevIntegrationBundle.message("addOn.install.popupTitle"))
                        .setAdText(DdevIntegrationBundle.message(SystemInfo.isMac
                                ? "addOn.install.openRepository.mac" : "addOn.install.openRepository"))
                        .setRenderer(new ColoredListCellRenderer<AddOn>() {
                            @Override
                            protected void customizeCellRenderer(@NotNull JList<? extends AddOn> list, AddOn addOn, int index, boolean selected, boolean hasFocus) {
                                this.append(String.valueOf(addOn.getTitle()), SimpleTextAttributes.REGULAR_ATTRIBUTES);

                                if (addOn.getDescription() != null) {
                                    this.append("  " + addOn.getDescription(), SimpleTextAttributes.GRAYED_ATTRIBUTES);
                                }
                            }
                        })
                        .setNamerForFiltering(addOn -> addOn.getTitle() + " " + addOn.getDescription())
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(addOn -> {
                            if (addOn.getTitle() != null) {
                                DdevRunner.getInstance().installAddOn(project, workingDirectory, addOn.getTitle(), afterCompletion);
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    public static void remove(@NotNull Project project, @NotNull String binary, @Nullable String workingDirectory,
                              @Nullable Runnable afterCompletion) {
        new Task.Backgroundable(project, DdevIntegrationBundle.message("addOn.loadingInstalled"), true) {
            private @Nullable List<InstalledAddOn> addOns;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.addOns = Ddev.getInstance().listInstalledAddOns(binary, project, workingDirectory);
                } catch (CommandFailedException exception) {
                    DdevNotifier.getInstance(project).notifyAddOnListFailed();
                }
            }

            @Override
            public void onSuccess() {
                if (this.addOns == null || this.addOns.isEmpty()) {
                    return;
                }

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.addOns)
                        .setTitle(DdevIntegrationBundle.message("addOn.remove.popupTitle"))
                        .setRenderer(new SimpleListCellRenderer<InstalledAddOn>() {
                            @Override
                            public void customize(@NotNull JList<? extends InstalledAddOn> list, InstalledAddOn addOn, int index, boolean selected, boolean hasFocus) {
                                this.setText(addOn.getName() + " (" + addOn.getVersion() + ")");
                            }
                        })
                        .setNamerForFiltering(addOn -> addOn.getName() + " " + addOn.getRepository())
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(addOn -> {
                            if (addOn.getName() != null) {
                                DdevRunner.getInstance().removeAddOn(project, workingDirectory, addOn.getName(), afterCompletion);
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }
}
