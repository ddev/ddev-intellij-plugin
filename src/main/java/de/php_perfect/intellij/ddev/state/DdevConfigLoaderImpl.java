package de.php_perfect.intellij.ddev.state;

import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        final String basePath = DdevProjectRoot.of(this.project);

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

    @Override
    public boolean exists() {
        final VirtualFile ddevConfig = this.load();

        return ddevConfig != null && ddevConfig.exists();
    }
}
