package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.SimpleListCellRenderer;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Snapshot;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

public final class DdevRestoreSnapshotAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
        final String binary = state.getDdevBinary();

        if (binary == null) {
            return;
        }

        new Task.Backgroundable(project, DdevIntegrationBundle.message("snapshot.loading"), true) {
            private @Nullable List<Snapshot> snapshots;
            private boolean failed = false;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.snapshots = Ddev.getInstance().listSnapshots(binary, project).stream()
                            .sorted(Comparator.comparing(Snapshot::getCreated, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                } catch (CommandFailedException exception) {
                    this.failed = true;
                    DdevNotifier.getInstance(project).notifySnapshotListFailed();
                }
            }

            @Override
            public void onSuccess() {
                if (this.failed || this.snapshots == null) {
                    return;
                }

                if (this.snapshots.isEmpty()) {
                    Messages.showInfoMessage(project,
                            DdevIntegrationBundle.message("snapshot.none.message"),
                            DdevIntegrationBundle.message("snapshot.restore.popupTitle"));
                    return;
                }

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.snapshots)
                        .setTitle(DdevIntegrationBundle.message("snapshot.restore.popupTitle"))
                        .setRenderer(new SimpleListCellRenderer<Snapshot>() {
                            @Override
                            public void customize(@NotNull JList<? extends Snapshot> list, Snapshot snapshot, int index, boolean selected, boolean hasFocus) {
                                this.setText(snapshot.getName());
                            }
                        })
                        .setNamerForFiltering(Snapshot::getName)
                        .setItemChosenCallback(snapshot -> {
                            if (snapshot.getName() != null) {
                                DdevRunner.getInstance().restoreSnapshot(project, snapshot.getName());
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }
}
