package de.php_perfect.intellij.ddev.state;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.messages.MessageBusConnection;
import de.php_perfect.intellij.ddev.DatabaseInfoChangedListener;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.StateInitializedListener;
import de.php_perfect.intellij.ddev.cmd.DatabaseInfo;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.MockProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

final class DdevStateManagerUpdateTest extends BasePlatformTestCase {
    private static final String STOPPED_DESCRIPTION = "{\"level\":\"info\",\"raw\":{\"name\":\"acol\",\"status\":\"stopped\"}}";
    private static final String RUNNING_DESCRIPTION = "{\"level\":\"info\",\"raw\":{\"name\":\"acol\",\"status\":\"running\",\"dbinfo\":"
            + "{\"database_type\":\"mysql\",\"database_version\":\"8.0\",\"dbPort\":3306,\"dbname\":\"db\",\"host\":\"db\","
            + "\"username\":\"db\",\"password\":\"db\",\"published_port\":0}}}";

    private MockProcessExecutor processExecutor;
    private MockDdevConfigLoader configLoader;
    private MessageBusConnection connection;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.processExecutor = (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
        this.configLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(this.getProject());
        this.connection = this.getProject().getMessageBus().connect(this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.configLoader.setExists(false);
            DdevSettingsState.getInstance(this.getProject()).ddevBinary = "";
            DdevStateManager.getInstance(this.getProject()).resetState();
        } finally {
            super.tearDown();
        }
    }

    @Test
    void unavailableDockerSkipsInitialization() {
        final AtomicInteger initialized = new AtomicInteger();
        this.connection.subscribe(StateInitializedListener.STATE_INITIALIZED, state -> initialized.incrementAndGet());
        this.processExecutor.addProcessOutput("docker info", new ProcessOutput(1));
        this.configLoader.setExists(true);

        DdevStateManager.getInstance(this.getProject()).initialize();

        Assertions.assertEquals(0, initialized.get());
        Assertions.assertFalse(DdevStateManager.getInstance(this.getProject()).getState().isConfigured());
    }

    @Test
    void updateConfigurationDetectsNewConfig() {
        final DdevStateManager stateManager = DdevStateManager.getInstance(this.getProject());
        final AtomicInteger changes = new AtomicInteger();
        this.connection.subscribe(StateChangedListener.DDEV_CHANGED, state -> {
            changes.incrementAndGet();
            stateManager.updateConfiguration();
        });
        this.configLoader.setExists(true);

        stateManager.updateConfiguration();

        Assertions.assertEquals(1, changes.get());
        Assertions.assertTrue(stateManager.getState().isConfigured());
        Assertions.assertNull(stateManager.getState().getDescription());
    }

    @Test
    void nestedUpdatesDuringDescriptionUpdateAreSkipped() {
        final DdevStateManager stateManager = this.initializeAvailableProject();
        final List<DatabaseInfo> databaseInfoChanges = new ArrayList<>();
        final AtomicInteger changes = new AtomicInteger();
        this.connection.subscribe(DatabaseInfoChangedListener.DATABASE_INFO_CHANGED_TOPIC, databaseInfoChanges::add);
        this.connection.subscribe(StateChangedListener.DDEV_CHANGED, state -> {
            changes.incrementAndGet();
            stateManager.updateDescription();
            stateManager.updateConfiguration();
        });
        this.processExecutor.addProcessOutput("ddev describe --json-output", new ProcessOutput(RUNNING_DESCRIPTION, "", 0, false, false));

        stateManager.updateDescription();

        final Description description = stateManager.getState().getDescription();
        Assertions.assertNotNull(description);
        Assertions.assertEquals(Description.Status.RUNNING, description.getStatus());
        Assertions.assertEquals(1, changes.get());
        Assertions.assertEquals(List.of(new DatabaseInfo(DatabaseInfo.Type.MYSQL, "8.0", 3306, "db", "db", "db", "db", 0)), databaseInfoChanges);
    }

    @Test
    void disposedProjectPublishesNoChanges() {
        final Project project = Mockito.mock(Project.class);
        final MockDdevConfigLoader loader = new MockDdevConfigLoader();
        loader.setExists(true);
        Mockito.when(project.isDisposed()).thenReturn(true);
        Mockito.when(project.getService(DdevConfigLoader.class)).thenReturn(loader);

        final DdevStateManagerImpl stateManager = new DdevStateManagerImpl(project);
        stateManager.updateConfiguration();

        Assertions.assertTrue(stateManager.getState().isConfigured());
        Mockito.verify(project, Mockito.never()).getMessageBus();
    }

    private DdevStateManager initializeAvailableProject() {
        DdevSettingsState.getInstance(this.getProject()).ddevBinary = "ddev";
        this.configLoader.setExists(true);
        this.processExecutor.addProcessOutput("docker info", new ProcessOutput(0));
        this.processExecutor.addProcessOutput("ddev --version", new ProcessOutput("ddev version v1.19.0", "", 0, false, false));
        this.processExecutor.addProcessOutput("ddev describe --json-output", new ProcessOutput(STOPPED_DESCRIPTION, "", 0, false, false));

        final DdevStateManager stateManager = DdevStateManager.getInstance(this.getProject());
        stateManager.initialize();

        final Description description = stateManager.getState().getDescription();
        Assertions.assertNotNull(description);
        Assertions.assertEquals(Description.Status.STOPPED, description.getStatus());

        return stateManager;
    }
}
