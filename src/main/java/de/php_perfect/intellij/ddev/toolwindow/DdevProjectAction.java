package de.php_perfect.intellij.ddev.toolwindow;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

abstract class DdevProjectAction extends DumbAwareAction {
    protected final @NotNull DdevProjectsPanel panel;

    DdevProjectAction(@NotNull DdevProjectsPanel panel, @NotNull String text, @Nullable Icon icon) {
        super(text, null, icon);
        this.panel = panel;
    }

    @Override
    public final void update(@NotNull AnActionEvent event) {
        event.getPresentation().setEnabledAndVisible(this.isEnabledFor(this.panel.getSelectedProject()));
    }

    @Override
    public final @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    protected boolean isEnabledFor(@Nullable DdevProject selected) {
        return selected != null && selected.getName() != null;
    }

    @Override
    public final void actionPerformed(@NotNull AnActionEvent event) {
        final DdevProject selected = this.panel.getSelectedProject();
        if (selected != null && selected.getName() != null) {
            this.perform(selected);
        }
    }

    protected abstract void perform(@NotNull DdevProject selected);
}
