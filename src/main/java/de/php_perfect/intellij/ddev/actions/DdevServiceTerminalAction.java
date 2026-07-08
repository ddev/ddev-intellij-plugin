package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalToolWindowManager;

import java.util.List;

public final class DdevServiceTerminalAction extends DdevAwareAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();

        if (project == null) {
            return;
        }

        final List<String> services = getServiceNames(project);

        if (services.isEmpty()) {
            return;
        }

        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(services)
                .setTitle(DdevIntegrationBundle.message("serviceTerminal.popupTitle"))
                .setNamerForFiltering(serviceName -> serviceName)
                .setFilterAlwaysVisible(true)
                .setItemChosenCallback(serviceName -> openServiceTerminal(project, serviceName))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    private static void openServiceTerminal(@NotNull Project project, @NotNull String serviceName) {
        final String tabTitle = "DDEV " + serviceName;

        DdevTerminalRunner runner = new DdevTerminalRunner(project, List.of("ssh", "-s", serviceName), tabTitle);
        TerminalTabState tabState = new TerminalTabState();
        tabState.myTabName = tabTitle;

        TerminalToolWindowManager.getInstance(project).createNewSession(runner, tabState);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return !getServiceNames(project).isEmpty();
    }

    private static @NotNull List<String> getServiceNames(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        if (!state.isAvailable() || !state.isConfigured()) {
            return List.of();
        }

        final Description description = state.getDescription();

        if (description == null || description.getStatus() != Description.Status.RUNNING) {
            return List.of();
        }

        return description.getServices().keySet().stream().sorted().toList();
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }
}
