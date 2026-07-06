package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Opens the project's custom PHP configuration ({@code .ddev/php/custom-php.ini}),
 * creating it with a small template first if it does not exist yet.
 */
public final class DdevEditPhpConfigAction extends DdevRunAction {
    private static final String TEMPLATE = """
            ; Custom PHP configuration for this DDEV project.
            ; Settings in this file are applied to the web container.
            ; Run "ddev restart" after changing this file.
            ;
            ; Example:
            ; [PHP]
            ; memory_limit = 512M
            """;

    @Override
    protected void run(@NotNull Project project) {
        final String basePath = project.getBasePath();

        if (basePath == null) {
            return;
        }

        final Path iniPath = Path.of(basePath, ".ddev", "php", "custom-php.ini");

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                if (!Files.exists(iniPath)) {
                    Files.createDirectories(iniPath.getParent());
                    Files.writeString(iniPath, TEMPLATE);
                }
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
