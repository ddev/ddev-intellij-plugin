package de.php_perfect.intellij.ddev.actions;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.ShareProviders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Starts a share with a provider picked for this share only. A {@code null} working directory
 * targets the IDE project.
 */
public final class ShareProviderChooser {
    private ShareProviderChooser() {
    }

    public static void share(@NotNull Project project, @Nullable String workingDirectory, @Nullable String docroot) {
        final String projectRoot = workingDirectory != null ? workingDirectory : DdevProjectRoot.of(project);

        JBPopupFactory.getInstance()
                .createPopupChooserBuilder(ShareProviders.list(projectRoot))
                .setTitle(DdevIntegrationBundle.message("share.provider.popupTitle"))
                .setItemChosenCallback(provider ->
                        DdevRunner.getInstance().share(project, workingDirectory, docroot, provider))
                .createPopup()
                .showCenteredInCurrentWindow(project);
    }
}
