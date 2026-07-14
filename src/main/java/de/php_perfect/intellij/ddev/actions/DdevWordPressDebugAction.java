package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.LocalFileSystem;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.wordpress.WordPressConfigManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

abstract class DdevWordPressDebugAction extends DdevRunAction {
    private final @NotNull WordPressConfigManager.DebugMode mode;

    DdevWordPressDebugAction(@NotNull WordPressConfigManager.DebugMode mode) {
        this.mode = mode;
    }

    @Override
    protected void run(@NotNull Project project) {
        final String basePath = project.getBasePath();

        if (basePath == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                final Path config = WordPressConfigManager.setDebugMode(Path.of(basePath), this.mode);
                LocalFileSystem.getInstance().refreshAndFindFileByNioFile(config);
            } catch (IOException exception) {
                ApplicationManager.getApplication().invokeLater(() -> Messages.showErrorDialog(
                        project,
                        DdevIntegrationBundle.message("wordpress.debug.failed.message", exception.getMessage()),
                        DdevIntegrationBundle.message("wordpress.debug.failed.title")
                ));
            }
        });
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        if (project.getBasePath() == null) {
            return false;
        }

        try {
            final WordPressConfigManager.DebugState state = WordPressConfigManager.readDebugState(
                    Path.of(Objects.requireNonNull(project.getBasePath())));
            return state != null && this.isApplicable(state);
        } catch (IOException exception) {
            return false;
        }
    }

    protected abstract boolean isApplicable(@NotNull WordPressConfigManager.DebugState state);
}
