package de.php_perfect.intellij.ddev.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbAwareAction;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevProject;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

final class DdevLifecycleActions {
    private DdevLifecycleActions() {
    }

    static final class ToggleCurrentProjectOnly extends ToggleAction implements DumbAware {
        private final @NotNull DdevProjectsPanel panel;

        ToggleCurrentProjectOnly(@NotNull DdevProjectsPanel panel) {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.toggleCurrentOnly"), null,
                    AllIcons.General.Filter);
            this.panel = panel;
        }

        @Override
        public boolean isSelected(@NotNull AnActionEvent event) {
            return this.panel.isShowingCurrentProjectOnly();
        }

        @Override
        public void setSelected(@NotNull AnActionEvent event, boolean state) {
            this.panel.setShowingCurrentProjectOnly(state);
        }

        @Override
        public @NotNull ActionUpdateThread getActionUpdateThread() {
            return ActionUpdateThread.EDT;
        }
    }

    static final class Refresh extends DumbAwareAction {
        private final @NotNull DdevProjectsPanel panel;

        Refresh(@NotNull DdevProjectsPanel panel) {
            super(DdevIntegrationBundle.message("toolWindow.projects.action.refresh"), null,
                    AllIcons.Actions.Refresh);
            this.panel = panel;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            this.panel.refresh();
        }
    }

    static final class Start extends DdevProjectAction {
        Start(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("toolWindow.projects.action.start"),
                    AllIcons.Actions.Execute);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && !selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().startProject(this.panel.project(),
                    Objects.requireNonNull(selected.getName()), this.panel::refreshLater);
        }
    }

    static final class Stop extends DdevProjectAction {
        Stop(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("toolWindow.projects.action.stop"),
                    AllIcons.Actions.Pause);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().stopProject(this.panel.project(),
                    Objects.requireNonNull(selected.getName()), this.panel::refreshLater);
        }
    }

    static final class Restart extends DdevProjectAction {
        Restart(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("toolWindow.projects.action.restart"),
                    AllIcons.Actions.Refresh);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && selected.isRunning();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            DdevRunner.getInstance().restartProject(this.panel.project(),
                    Objects.requireNonNull(selected.getName()), this.panel::refreshLater);
        }
    }

    static final class StopOthers extends DdevProjectAction {
        StopOthers(@NotNull DdevProjectsPanel panel) {
            super(panel, DdevIntegrationBundle.message("toolWindow.projects.action.stopOthers"),
                    AllIcons.Actions.Suspend);
        }

        @Override
        protected boolean isEnabledFor(@Nullable DdevProject selected) {
            return super.isEnabledFor(selected) && !this.panel.collectOtherRunning(selected).isEmpty();
        }

        @Override
        protected void perform(@NotNull DdevProject selected) {
            final List<String> projects = this.panel.collectOtherRunning(selected);
            DdevRunner.getInstance().stopProjects(this.panel.project(), projects, this.panel::refreshLater);
        }
    }
}
