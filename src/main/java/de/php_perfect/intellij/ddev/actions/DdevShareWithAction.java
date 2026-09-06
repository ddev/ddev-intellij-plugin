package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.cmd.ShareManager;
import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;

public final class DdevShareWithAction extends DdevRunningAction {
    @Override
    protected void run(@NotNull Project project) {
        ShareProviderChooser.share(project, null, WordPressConfigManager.docroot(project));
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return super.isActive(project) && !ShareManager.getInstance(project).isSharing();
    }
}
