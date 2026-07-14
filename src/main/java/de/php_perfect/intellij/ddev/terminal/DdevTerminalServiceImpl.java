package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.List;

public final class DdevTerminalServiceImpl implements DdevTerminalService {
    private final @NotNull Project project;

    public DdevTerminalServiceImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void open(@NotNull List<String> ddevArguments, @NotNull String title,
                     @Nullable String workingDirectory) {
        final DdevTerminalRunner runner = new DdevTerminalRunner(this.project, ddevArguments, title, workingDirectory);
        final TerminalTabState tabState = new TerminalTabState();
        tabState.myTabName = title;
        TerminalToolWindowManager.getInstance(this.project).createNewSession(runner, tabState);
    }
}
