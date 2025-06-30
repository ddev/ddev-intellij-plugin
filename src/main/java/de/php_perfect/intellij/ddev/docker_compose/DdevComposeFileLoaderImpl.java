package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class DdevComposeFileLoaderImpl implements DdevComposeFileLoader {
    private static final @NotNull String DDEV_COMPOSE_PATH = ".ddev/.ddev-docker-compose-full.yaml";

    private final @NotNull Project project;

    public DdevComposeFileLoaderImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nullable VirtualFile load() {
        final String basePath = this.project.getBasePath();

        if (basePath == null) {
            return null;
        }

        final Path path = Paths.get(basePath, DDEV_COMPOSE_PATH);

        return VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path);
    }
}
