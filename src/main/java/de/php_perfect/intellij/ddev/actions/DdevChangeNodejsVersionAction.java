package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptions;
import de.php_perfect.intellij.ddev.cmd.DdevConfigOptionsLoader;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevChangeNodejsVersionAction extends DdevRunAction {
    private static final String CUSTOM = DdevIntegrationBundle.message("changeNodejsVersion.custom");

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
                final java.util.List<String> versions = new java.util.ArrayList<>(this.options.nodejsVersions());
                versions.add(CUSTOM);
                JBPopupFactory.getInstance()
                        .createPopupChooserBuilder(versions)
                        .setTitle(DdevIntegrationBundle.message("changeNodejsVersion.title"))
                        .setNamerForFiltering(version -> version)
                        .setFilterAlwaysVisible(true)
                        .setItemChosenCallback(version -> {
                            if (CUSTOM.equals(version)) {
                                chooseCustomVersion(project);
                            } else {
                                DdevRunner.getInstance().updateConfig(project, "--nodejs-version=" + version);
                            }
                        })
                        .createPopup()
                        .showCenteredInCurrentWindow(project);
            }
        }.queue();
    }

    private static void chooseCustomVersion(@NotNull Project project) {
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
