package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.cmd.DdevConfigFiles;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Opens the project's custom PHP configuration ({@code .ddev/php/custom-php.ini}),
 * creating it with a small template first if it does not exist yet.
 */
public final class DdevEditPhpConfigAction extends DdevRunAction {
    @Override
    protected void run(@NotNull Project project) {
        final String basePath = project.getBasePath();

        if (basePath == null) {
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            final Path iniPath;

            try {
                iniPath = DdevConfigFiles.ensureCustomPhpIni(Path.of(basePath));
            } catch (IOException ignored) {
                return;
            }

            ApplicationManager.getApplication().invokeLater(() -> {
                final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(iniPath);

                if (virtualFile != null) {
                    FileEditorManager.getInstance(project).openFile(virtualFile, true);
                }
            });
        });
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        final State state = DdevStateManager.getInstance(project).getState();

        return state.isAvailable() && state.isConfigured();
    }
}
