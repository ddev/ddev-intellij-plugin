package de.php_perfect.intellij.ddev.actions;

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
import de.php_perfect.intellij.ddev.cmd.Snapshot;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Deletes a single database snapshot. DDEV has no CLI command for this,
 * so the snapshot files are removed from {@code .ddev/db_snapshots} directly.
 */
public final class DdevDeleteSnapshotAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();
        final String binary = state.getDdevBinary();

        if (binary == null || project.getBasePath() == null) {
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
                            DdevIntegrationBundle.message("snapshot.delete.popupTitle"));
                    return;
                }

                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.snapshots)
                        .setTitle(DdevIntegrationBundle.message("snapshot.delete.popupTitle"))
                        .setRenderer(new SimpleListCellRenderer<Snapshot>() {
                            @Override
                            public void customize(@NotNull JList<? extends Snapshot> list, Snapshot snapshot, int index, boolean selected, boolean hasFocus) {
                                this.setText(snapshot.getName());
                            }
                        })
                        .setNamerForFiltering(Snapshot::getName)
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(snapshot -> DdevDeleteSnapshotAction.confirmAndDelete(project, snapshot))
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    private static void confirmAndDelete(@NotNull Project project, @NotNull Snapshot snapshot) {
        final String name = snapshot.getName();

        if (name == null) {
            return;
        }

        final boolean confirmed = MessageDialogBuilder.yesNo(
                DdevIntegrationBundle.message("snapshot.delete.confirm.title"),
                DdevIntegrationBundle.message("snapshot.delete.confirm.message", name)
        ).ask(project);

        if (!confirmed) {
            return;
        }

        final Path snapshotDir = Path.of(java.util.Objects.requireNonNull(project.getBasePath()), ".ddev", "db_snapshots");
        // Snapshot files are named "<name>-<dbtype>_<dbversion>.<ext>"; legacy snapshots were directories named "<name>".
        final Pattern filePattern = Pattern.compile(Pattern.quote(name) + "-(mysql|mariadb|postgres)_.+");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(snapshotDir)) {
                for (final Path entry : entries) {
                    final String fileName = entry.getFileName().toString();

                    if (fileName.equals(name) || filePattern.matcher(fileName).matches()) {
                        deleteRecursively(entry);
                    }
                }
            } catch (IOException ignored) {
                // Directory missing or not readable; nothing to delete.
            }

            LocalFileSystem.getInstance().refreshAndFindFileByNioFile(snapshotDir);
        });
    }

    private static void deleteRecursively(@NotNull Path path) throws IOException {
        try (Stream<Path> paths = Files.walk(path)) {
            for (final Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(p);
            }
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
