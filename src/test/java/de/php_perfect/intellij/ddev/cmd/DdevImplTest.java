package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.execution.wsl.WslPath;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.version.Version;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.*;

final class DdevImplTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void version() throws CommandFailedException {
        final Version expected = new Version("v1.22.0");
        final ProcessOutput processOutput = new ProcessOutput("ddev version v1.22.0", "", 0, false, false);

        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev --version", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().version("ddev", getProject()));
    }

    @Test
    void headVersion() throws CommandFailedException {
        final Version expected = new Version("v1.24.4-36-ge74e3a95f");
        final ProcessOutput processOutput = new ProcessOutput("ddev version v1.24.4-36-ge74e3a95f", "", 0, false, false);

        final MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev --version", processOutput);

        Version actual = new DdevImpl().version("ddev", getProject());
        Assertions.assertEquals(expected, actual);
        Assertions.assertTrue(actual.isHeadVersion());
        Assertions.assertEquals("36-ge74e3a95f", actual.getBuildInfo());
    }

    @Test
    void detailedVersions() throws CommandFailedException, IOException {
        Versions expected = new Versions("v1.19.0", "20.10.12", "v2.2.2", "docker-desktop");

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_version.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev version --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().detailedVersions("ddev", getProject()));
    }

    @Test
    void listAddOns() throws CommandFailedException, IOException {
        final List<AddOn> expected = List.of(
                new AddOn("ddev/ddev-redis", "Redis service for DDEV", "official", "v2.1.0"),
                new AddOn("2ndkauboy/ddev-elasticvue", "Elasticvue service for DDEV", "contrib", "1.1.0")
        );

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_addon_list.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev add-on list --all --wrap-table --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().listAddOns("ddev", getProject()));
    }

    @Test
    void listAddOnsFromCurrentTableEnvelope() throws CommandFailedException {
        final String output = "{\"level\":\"info\",\"msg\":\"│ ADD-ON │ DESCRIPTION │\\n"
                + "│ ddev/ddev-redis │ Redis service for DDEV │\"}";
        final MockProcessExecutor executor = (MockProcessExecutor) ApplicationManager.getApplication()
                .getService(ProcessExecutor.class);
        executor.addProcessOutput("ddev add-on list --all --wrap-table --json-output",
                new ProcessOutput(output, "", 0, false, false));

        Assertions.assertEquals(List.of(new AddOn(
                "ddev/ddev-redis", "Redis service for DDEV", "official", null)),
                new DdevImpl().listAddOns("ddev", getProject()));
    }

    @Test
    void listInstalledAddOns() throws CommandFailedException, IOException {
        final List<InstalledAddOn> expected = List.of(
                new InstalledAddOn("adminer", "ddev/ddev-adminer", "v1.3.1")
        );

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_addon_list_installed.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev add-on list --installed --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().listInstalledAddOns("ddev", getProject()));
    }

    @Test
    void listProjects() throws CommandFailedException, IOException {
        final List<DdevProject> expected = List.of(
                new DdevProject("alpha", "/home/user/Projects/alpha", "~/Projects/alpha", Description.Status.RUNNING, "running", "laravel", "https://alpha.ddev.site", "public"),
                new DdevProject("beta", "/home/user/Projects/beta", "~/Projects/beta", Description.Status.STOPPED, "stopped", "drupal11", "https://beta.ddev.site", "web")
        );

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_list.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev list --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().listProjects("ddev", getProject()));
    }

    @Test
    void listsWslProjectsWithHostAccessibleRootsAndDocumentRoots() throws Exception {
        final String base = "\\\\wsl.localhost\\Ubuntu\\home\\user\\current";
        final Project project = mock(Project.class);
        when(project.getBasePath()).thenReturn(base);
        final MockProcessExecutor executor = (MockProcessExecutor) ProcessExecutor.getInstance();
        executor.addProcessOutput("ddev list --json-output", new ProcessOutput(
                Files.readString(Path.of("src/test/resources/ddev_list.json")), "", 0, false, false));
        try (var paths = mockStatic(WslPath.class)) {
            paths.when(() -> WslPath.parseWindowsUncPath(base))
                    .thenReturn(new WslPath("\\\\wsl.localhost\\", "Ubuntu", "/home/user/current"));
            final List<DdevProject> projects = new DdevImpl().listProjects("ddev", project);
            Assertions.assertEquals("\\\\wsl.localhost\\Ubuntu\\home\\user\\Projects\\alpha", projects.getFirst().getAppRoot());
            Assertions.assertEquals("public", projects.getFirst().getDocroot());
            Assertions.assertEquals("web", projects.getLast().getDocroot());
        }
    }

    @Test
    void listSnapshots() throws CommandFailedException, IOException {
        final List<Snapshot> expected = List.of(
                new Snapshot("claude-test-snap", "2026-07-06T08:15:32.93905135+02:00"),
                new Snapshot("older-snap", "2026-07-01T10:00:00.00000000+02:00")
        );

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_snapshot_list.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev snapshot --list --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().listSnapshots("ddev", getProject()));
    }

    @Test
    void listSnapshotsEmpty() throws CommandFailedException, IOException {
        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_snapshot_list_empty.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev snapshot --list --json-output", processOutput);

        Assertions.assertEquals(List.of(), new DdevImpl().listSnapshots("ddev", getProject()));
    }

    @Test
    void describe() throws CommandFailedException, IOException {
        Description expected = Description.builder()
                .name("acol")
                .docroot("public")
                .phpVersion("8.1")
                .status(Description.Status.STOPPED)
                .services(new HashMap<>())
                .primaryUrl("https://acol.ddev.site")
                .build();

        ProcessOutput processOutput = new ProcessOutput(Files.readString(Path.of("src/test/resources/ddev_describe.json")), "", 0, false, false);

        MockProcessExecutor mockProcessExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        mockProcessExecutor.addProcessOutput("ddev describe --json-output", processOutput);

        Assertions.assertEquals(expected, new DdevImpl().describe("ddev", getProject()));
    }
}
