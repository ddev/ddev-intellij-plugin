package de.php_perfect.intellij.ddev.actions;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.ui.SimpleListCellRenderer;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Snapshot;
import de.php_perfect.intellij.ddev.cmd.SnapshotFileManager;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

/**
 * Lets the user pick one of a project's database snapshots. A {@code null} working directory
 * targets the IDE project; any other directory targets that DDEV project instead.
 */
public final class SnapshotChooser {
    private SnapshotChooser() {
    }

    public static void restore(@NotNull Project project, @NotNull String binary, @Nullable String workingDirectory) {
        choose(project, binary, workingDirectory, DdevIntegrationBundle.message("snapshot.restore.popupTitle"), snapshot -> {
            if (snapshot.getName() != null) {
                DdevRunner.getInstance().restoreSnapshot(project, workingDirectory, snapshot.getName());
            }
        });
    }

    /**
     * DDEV has no CLI command for deleting a single snapshot, so the snapshot files are removed
     * from {@code .ddev/db_snapshots} directly.
     */
    public static void delete(@NotNull Project project, @NotNull String binary, @Nullable String workingDirectory) {
        final String projectRoot = workingDirectory != null ? workingDirectory : DdevProjectRoot.of(project);

        if (projectRoot == null) {
            return;
        }

        choose(project, binary, workingDirectory, DdevIntegrationBundle.message("snapshot.delete.popupTitle"),
                snapshot -> confirmAndDelete(project, Path.of(projectRoot), snapshot));
    }

    private static void choose(@NotNull Project project, @NotNull String binary, @Nullable String workingDirectory,
                               @NotNull String title, @NotNull Consumer<Snapshot> onChosen) {
        new Task.Backgroundable(project, DdevIntegrationBundle.message("snapshot.loading"), true) {
            private @Nullable List<Snapshot> snapshots;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.snapshots = Ddev.getInstance().listSnapshots(binary, project, workingDirectory).stream()
                            .sorted(Comparator.comparing(Snapshot::getCreated, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                } catch (CommandFailedException exception) {
                    DdevNotifier.getInstance(project).notifySnapshotListFailed();
                }
            }

            @Override
            public void onSuccess() {
                if (this.snapshots == null) {
                    return;
                }

                if (this.snapshots.isEmpty()) {
                    Messages.showInfoMessage(project, DdevIntegrationBundle.message("snapshot.none.message"), title);
                    return;
                }

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.snapshots)
                        .setTitle(title)
                        .setRenderer(new SimpleListCellRenderer<Snapshot>() {
                            @Override
                            public void customize(@NotNull JList<? extends Snapshot> list, Snapshot snapshot, int index, boolean selected, boolean hasFocus) {
                                this.setText(snapshot.getName());
                            }
                        })
                        .setNamerForFiltering(Snapshot::getName)
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(onChosen::accept)
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    private static void confirmAndDelete(@NotNull Project project, @NotNull Path projectRoot, @NotNull Snapshot snapshot) {
        final String name = snapshot.getName();

        if (name == null || !MessageDialogBuilder.yesNo(
                DdevIntegrationBundle.message("snapshot.delete.confirm.title"),
                DdevIntegrationBundle.message("snapshot.delete.confirm.message", name)
        ).ask(project)) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (!SnapshotFileManager.deleteSnapshot(projectRoot, name)) {
                    DdevNotifier.getInstance(project).notifySnapshotDeleteFailed(name,
                            DdevIntegrationBundle.message("snapshot.delete.notFound"));
                }
            } catch (IOException exception) {
                DdevNotifier.getInstance(project).notifySnapshotDeleteFailed(name, String.valueOf(exception.getMessage()));
            }

            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectRoot.resolve(".ddev").resolve("db_snapshots"));
        });
    }
}
