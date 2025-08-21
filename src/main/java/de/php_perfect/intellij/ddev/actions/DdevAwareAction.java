package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsActions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

abstract class DdevAwareAction extends DumbAwareAction {
    DdevAwareAction() {
        super();
    }

    DdevAwareAction(@Nullable @NlsActions.ActionText String text, @Nullable @NlsActions.ActionDescription String description, @Nullable Icon icon) {
        super(text, description, icon);
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        final Project project = e.getProject();

        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        try {
            e.getPresentation().setEnabled(this.isActive(project));
        } catch (Exception ex) {
            // if state access fails, disable the action
            e.getPresentation().setEnabled(false);
        }
    }

    protected abstract boolean isActive(@NotNull Project project);
}
