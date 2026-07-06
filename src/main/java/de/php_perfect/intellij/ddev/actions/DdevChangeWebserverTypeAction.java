package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class DdevChangeWebserverTypeAction extends DdevRunAction {
    private static final List<String> WEBSERVER_TYPES = List.of("nginx-fpm", "apache-fpm", "generic");

    @Override
    protected void run(@NotNull Project project) {
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(WEBSERVER_TYPES)
                .setTitle(DdevIntegrationBundle.message("changeWebserverType.popupTitle"))
                .setItemChosenCallback(type -> DdevRunner.getInstance().updateConfig(project, "--webserver-type=" + type))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
