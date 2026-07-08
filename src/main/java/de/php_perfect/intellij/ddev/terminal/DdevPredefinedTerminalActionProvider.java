package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ui.OpenPredefinedTerminalActionProvider;

import java.util.List;

public final class DdevPredefinedTerminalActionProvider implements OpenPredefinedTerminalActionProvider {
    @Override
    public @NotNull List<AnAction> listOpenPredefinedTerminalActions(@NotNull Project project) {
        State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable() || !state.isConfigured()) {
            return List.of();
        }

        return List.of(
                ActionManager.getInstance().getAction("DdevIntegration.Terminal"),
                ActionManager.getInstance().getAction("DdevIntegration.DatabaseTerminal"),
                ActionManager.getInstance().getAction("DdevIntegration.ServiceTerminal")
        );
    }
}
