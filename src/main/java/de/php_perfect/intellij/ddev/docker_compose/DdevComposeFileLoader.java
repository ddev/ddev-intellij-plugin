package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface DdevComposeFileLoader {
    @Nullable VirtualFile load();

    static DdevComposeFileLoader getInstance(@NotNull Project project) {
        return project.getService(DdevComposeFileLoader.class);
    }
}
