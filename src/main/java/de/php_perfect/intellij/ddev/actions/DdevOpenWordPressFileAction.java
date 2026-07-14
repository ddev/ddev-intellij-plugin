package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;

abstract class DdevOpenWordPressFileAction extends DdevRunAction {
    private final @NotNull String relativePath;

    DdevOpenWordPressFileAction(@NotNull String relativePath) {
        this.relativePath = relativePath;
    }

    @Override
    protected void run(@NotNull Project project) {
        final Path file = this.resolve(project);

        if (file == null) {
            return;
        }

        final VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(file);

        if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true);
        }
    }

    @Override
    protected boolean isActive(@NotNull Project project) {
        return this.resolve(project) != null;
    }

    private Path resolve(@NotNull Project project) {
        final String basePath = project.getBasePath();

        if (basePath == null) {
            return null;
        }

        final Path path = Path.of(basePath).resolve(this.relativePath);
        return Files.isRegularFile(path) ? path : null;
    }
}
