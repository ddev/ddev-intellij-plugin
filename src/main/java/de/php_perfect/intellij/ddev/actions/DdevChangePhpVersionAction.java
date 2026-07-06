package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DdevChangePhpVersionAction extends DdevRunAction {
    private static final List<String> PHP_VERSIONS = List.of(
            "8.4", "8.3", "8.2", "8.1", "8.0", "7.4", "7.3", "7.2", "7.1", "7.0", "5.6"
    );

    @Override
    protected void run(@NotNull Project project) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(PHP_VERSIONS)
                .setTitle(DdevIntegrationBundle.message("changePhpVersion.popupTitle"))
                .setItemChosenCallback(version -> DdevRunner.getInstance().updateConfig(project, "--php-version=" + version))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
