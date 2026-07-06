package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

public final class DdevDeleteImagesAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final boolean confirmed = MessageDialogBuilder.yesNo(
                DdevIntegrationBundle.message("dialog.deleteImages.title"),
                DdevIntegrationBundle.message("dialog.deleteImages.message")
        ).ask(project);

        if (confirmed) {
            DdevRunner.getInstance().deleteImages(project);
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable();
    }
}
