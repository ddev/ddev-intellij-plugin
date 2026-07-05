package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

final class DdevConfigLoaderTest extends BasePlatformTestCase {
    private final @NotNull List<File> files = new ArrayList<>();

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        // The reused light project can live in a previous test's temp directory, which is not
        // among the allowed VFS roots of this test. On macOS the canonical form additionally
        // differs from the path itself (/private/var/... vs /var/...), so both must be allowed
        // or VFS lookups fail with VfsRootAccessNotAllowedError.
        final String basePath = this.getProject().getBasePath();
        if (basePath != null) {
            VfsRootAccess.allowRootAccess(this.getTestRootDisposable(), basePath, new File(basePath).getCanonicalPath());
        }
    }

    @Test
    void nonExistentConfig() {
        Assertions.assertFalse(new DdevConfigLoaderImpl(this.getProject()).exists());
    }

    @Test
    void existentConfig() {
        Project project = this.getProject();

        File ddevConfig = new File(project.getBasePath() + "/.ddev/config.yaml");
        this.files.add(ddevConfig);
        ddevConfig.deleteOnExit();

        try {
            FileUtil.writeToFile(ddevConfig, "name: test", true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Assertions.assertTrue(new DdevConfigLoaderImpl(project).exists());
    }

    @Test
    void loadNonExisting() {
        Assertions.assertNull(new DdevConfigLoaderImpl(this.getProject()).load());
    }

    @Test
    void loadExisting() {
        Project project = this.getProject();

        File ddevConfig = new File(project.getBasePath() + "/.ddev/config.yaml");
        this.files.add(ddevConfig);
        ddevConfig.deleteOnExit();

        try {
            FileUtil.writeToFile(ddevConfig, "name: test", true);
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        Assertions.assertInstanceOf(VirtualFile.class, new DdevConfigLoaderImpl(project).load());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        for (File file : this.files) {
            Files.deleteIfExists(file.toPath());
        }

        super.tearDown();
    }
}
