package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.List;

public final class DdevDatabaseTerminalAction extends DdevAwareAction {
    private static final String TAB_TITLE = "DDEV Database";

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        final DatabaseInfo databaseInfo = getDatabaseInfo(project);

        if (databaseInfo == null) {
            return;
        }

        final String databaseClient = databaseInfo.type() == DatabaseInfo.Type.POSTGRESQL ? "psql" : "mysql";

        DdevTerminalRunner runner = new DdevTerminalRunner(project, List.of(databaseClient), TAB_TITLE);
        TerminalTabState tabState = new TerminalTabState();
        tabState.myTabName = TAB_TITLE;

        TerminalToolWindowManager.getInstance(project).createNewSession(runner, tabState);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return getDatabaseInfo(project) != null;
    }

    private static @Nullable DatabaseInfo getDatabaseInfo(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable() || !state.isConfigured()) {
            return null;
        }

        final Description description = state.getDescription();

        if (description == null || description.getStatus() != Description.Status.RUNNING) {
            return null;
        }

        return description.getDatabaseInfo();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
