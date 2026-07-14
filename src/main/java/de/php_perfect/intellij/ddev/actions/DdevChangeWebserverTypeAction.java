package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptions;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptionsLoader;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevChangeWebserverTypeAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        new Task.Backgroundable(project, DdevIntegrationBundle.message("configOptions.loading"), true) {
            private DdevConfigOptions options;

            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                this.options = DdevConfigOptionsLoader.getInstance().load(indicator);
            }

            @Override
            public void onSuccess() {
                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(this.options.webserverTypes())
                        .setTitle(DdevIntegrationBundle.message("changeWebserverType.popupTitle"))
                        .setItemChosenCallback(type -> DdevRunner.getInstance().updateConfig(project, "--webserver-type=" + type))
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
