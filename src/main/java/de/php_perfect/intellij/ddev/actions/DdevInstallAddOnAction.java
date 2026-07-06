package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.SimpleListCellRenderer;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.AddOn;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

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
                    this.addOns = Ddev.getInstance().listAddOns(binary, project);
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
                        .setRenderer(new SimpleListCellRenderer<AddOn>() {
                            @Override
                            public void customize(@NotNull JList<? extends AddOn> list, AddOn addOn, int index, boolean selected, boolean hasFocus) {
                                this.setText(addOn.getTitle());
                                this.setToolTipText(addOn.getDescription());
                            }
                        })
                        .setNamerForFiltering(addOn -> addOn.getTitle() + " " + addOn.getDescription())
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
