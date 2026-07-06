package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevChangeNodejsVersionAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final String version = Messages.showInputDialog(
                project,
                DdevIntegrationBundle.message("changeNodejsVersion.message"),
                DdevIntegrationBundle.message("changeNodejsVersion.title"),
                null
        );

        if (version != null && !version.isBlank()) {
            DdevRunner.getInstance().updateConfig(project, "--nodejs-version=" + version.trim());
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
