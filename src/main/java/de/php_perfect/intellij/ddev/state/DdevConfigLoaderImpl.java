package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class DdevConfigLoaderImpl implements DdevConfigLoader {
    private static final @NotNull String DDEV_CONFIG_PATH = ".ddev/config.yaml";
    private final @NotNull Project project;

    public DdevConfigLoaderImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @Nullable VirtualFile load() {
        final String basePath = this.project.getBasePath();

        if (basePath == null) {
            return null;
        }

        final Path path = Paths.get(basePath, DDEV_CONFIG_PATH);

        VirtualFile config = VirtualFileManager.getInstance().refreshAndFindFileByNioPath(path);

        if (config == null) {
            return null;
        }

        config.refresh(false, false);

        return config;
    }

    /**
     * Checked on the file system directly, since the state watcher calls this every few seconds and a VFS
     * refresh on each call is expensive and fails while the VFS is being rebuilt.
     */
    @Override
    public boolean exists() {
        final String basePath = this.project.getBasePath();

        if (basePath == null) {
            return false;
        }

        try {
            return Files.isRegularFile(Paths.get(basePath, DDEV_CONFIG_PATH));
        } catch (InvalidPathException exception) {
            return false;
        }
    }
}
