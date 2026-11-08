package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

final class DdevComposeFileLoaderImplTest extends BasePlatformTestCase {
    private File composeFile;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        final String basePath = this.getProject().getBasePath();
        if (basePath != null) {
            VfsRootAccess.allowRootAccess(this.getTestRootDisposable(), basePath, new File(basePath).getCanonicalPath());
        }
        this.composeFile = new File(basePath, ".ddev/.ddev-docker-compose-full.yaml");
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            Files.deleteIfExists(this.composeFile.toPath());
            LocalFileSystem.getInstance().refreshNioFiles(List.of(this.composeFile.toPath()));
        } finally {
            super.tearDown();
        }
    }

    @Test
    void loadsExistingComposeFile() throws Exception {
        FileUtil.writeToFile(this.composeFile, "services: {}");

        final VirtualFile file = new DdevComposeFileLoaderImpl(this.getProject()).load();

        Assertions.assertNotNull(file);
        Assertions.assertEquals(".ddev-docker-compose-full.yaml", file.getName());
    }

    @Test
    void missingComposeFileIsNull() {
        Assertions.assertNull(new DdevComposeFileLoaderImpl(this.getProject()).load());
    }

    @Test
    void projectWithoutBasePathHasNoComposeFile() {
        Assertions.assertNull(new DdevComposeFileLoaderImpl(Mockito.mock(Project.class)).load());
    }
}
