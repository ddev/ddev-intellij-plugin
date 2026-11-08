package de.php_perfect.intellij.ddev.node;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.docker_compose.DdevComposeFileLoader;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class AutoConfigureNodeInterpreterListenerTest extends BasePlatformTestCase {
    private static final String COMPOSE_FILE = "/project/.ddev/.ddev-docker-compose-full.yaml";

    private DdevComposeFileLoader composeFileLoader;
    private NodeInterpreterProvider interpreterProvider;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.composeFileLoader = Mockito.mock(DdevComposeFileLoader.class);
        this.interpreterProvider = Mockito.mock(NodeInterpreterProvider.class);
        ServiceContainerUtil.replaceService(this.getProject(), DdevComposeFileLoader.class, this.composeFileLoader, this.getTestRootDisposable());
        ServiceContainerUtil.replaceService(this.getProject(), NodeInterpreterProvider.class, this.interpreterProvider, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(this.getProject()).autoConfigureNodeJsInterpreter = true;
        super.tearDown();
    }

    @Test
    void configuresInterpreterForExistingComposeFile() {
        this.mockComposeFile(true);

        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(description("acol"));

        Mockito.verify(this.interpreterProvider).configureNodeInterpreter(new NodeInterpreterConfig("acol", COMPOSE_FILE, "node"));
    }

    @Test
    void disposedProjectIsIgnored() {
        final Project project = Mockito.mock(Project.class);
        Mockito.when(project.isDisposed()).thenReturn(true);

        new AutoConfigureNodeInterpreterListener(project).onDescriptionChanged(description("acol"));

        Mockito.verify(project, Mockito.never()).getService(Mockito.any());
    }

    @Test
    void missingDescriptionIsIgnored() {
        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(null);

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void missingNameIsIgnored() {
        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(description(null));

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void disabledSettingIsRespected() {
        DdevSettingsState.getInstance(this.getProject()).autoConfigureNodeJsInterpreter = false;

        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(description("acol"));

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void missingComposeFileIsIgnored() {
        Mockito.when(this.composeFileLoader.load()).thenReturn(null);

        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(description("acol"));

        Mockito.verifyNoInteractions(this.interpreterProvider);
    }

    @Test
    void deletedComposeFileIsIgnored() {
        this.mockComposeFile(false);

        new AutoConfigureNodeInterpreterListener(this.getProject()).onDescriptionChanged(description("acol"));

        Mockito.verifyNoInteractions(this.interpreterProvider);
    }

    private void mockComposeFile(boolean exists) {
        final VirtualFile composeFile = Mockito.mock(VirtualFile.class);
        Mockito.when(composeFile.exists()).thenReturn(exists);
        Mockito.when(composeFile.getPath()).thenReturn(COMPOSE_FILE);
        Mockito.when(this.composeFileLoader.load()).thenReturn(composeFile);
    }

    private static Description description(String name) {
        return Description.builder().name(name).build();
    }
}
