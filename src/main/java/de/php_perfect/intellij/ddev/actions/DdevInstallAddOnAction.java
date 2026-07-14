package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.AddOn;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.InstalledAddOn;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class DdevInstallAddOnAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
        final String binary = state.getDdevBinary();

        if (binary == null) {
            return;
        }

        new Task.Backgroundable(project, DdevIntegrationBundle.message("addOn.loadingAvailable"), true) {
            private @Nullable List<AddOn> addOns;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    final Set<String> installedRepositories = Ddev.getInstance().listInstalledAddOns(binary, project).stream()
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

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.addOns)
                        .setTitle(DdevIntegrationBundle.message("addOn.install.popupTitle"))
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
                                DdevRunner.getInstance().installAddOn(project, addOn.getTitle());
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
