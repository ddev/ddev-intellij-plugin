package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.SimpleListCellRenderer;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.CommandFailedException;
import de.php_perfect.intellij.ddev.cmd.Ddev;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.util.DdevProjectOpener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Comparator;
import java.util.List;

/**
 * Lists all DDEV projects and opens the chosen one as an IDE project.
 */
public abstract class DdevOpenProjectAction extends DumbAwareAction {
    protected abstract boolean inNewWindow();

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        final String binary = project == null ? null : DdevStateManager.getInstance(project).getState().getDdevBinary();

        if (binary == null) {
            return;
        }

        new Task.Backgroundable(project, DdevIntegrationBundle.message("toolWindow.projects.loading"), true) {
            private @Nullable List<DdevProject> projects;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                try {
                    this.projects = Ddev.getInstance().listProjects(binary, project).stream()
                            .filter(ddevProject -> ddevProject.getAppRoot() != null && ddevProject.getName() != null)
                            .sorted(Comparator.comparing(DdevProject::getName, String.CASE_INSENSITIVE_ORDER))
                            .toList();
                } catch (CommandFailedException exception) {
                    this.projects = null;
                }
            }

            @Override
            public void onSuccess() {
                if (this.projects == null || this.projects.isEmpty()) {
                    return;
                }
                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.projects)
                        .setTitle(DdevIntegrationBundle.message(DdevOpenProjectAction.this.inNewWindow()
                                ? "openProject.newWindow.popupTitle" : "openProject.popupTitle"))
                        .setRenderer(new SimpleListCellRenderer<DdevProject>() {
                            @Override
                            public void customize(@NotNull JList<? extends DdevProject> list, DdevProject ddevProject,
                                                  int index, boolean selected, boolean hasFocus) {
                                this.setText(ddevProject.getName() + "  —  " + ddevProject.getAppRoot());
                            }
                        })
                        .setNamerForFiltering(DdevProject::getName)
                        .setItemChosenCallback(ddevProject -> {
                            if (DdevOpenProjectAction.this.inNewWindow()) {
                                DdevProjectOpener.openInNewWindow(ddevProject.getAppRoot());
                            } else {
                                DdevProjectOpener.openInCurrentWindow(ddevProject.getAppRoot());
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();
        e.getPresentation().setEnabled(project != null
                && DdevStateManager.getInstance(project).getState().isAvailable());
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    public static final class InCurrentWindow extends DdevOpenProjectAction {
        @Override
        protected boolean inNewWindow() {
            return false;
        }
    }

    public static final class InNewWindow extends DdevOpenProjectAction {
        @Override
        protected boolean inNewWindow() {
            return true;
        }
    }
}
