package de.php_perfect.intellij.ddev.php;

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

final class ConfigurationProviderImplTest extends BasePlatformTestCase {
    private static final String COMPOSE_FILE = "/project/.ddev/.ddev-docker-compose-full.yaml";

    private DdevComposeFileLoader composeFileLoader;
    private PhpInterpreterProvider interpreterProvider;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.composeFileLoader = Mockito.mock(DdevComposeFileLoader.class);
        this.interpreterProvider = Mockito.mock(PhpInterpreterProvider.class);
        ServiceContainerUtil.replaceService(this.getProject(), DdevComposeFileLoader.class, this.composeFileLoader, this.getTestRootDisposable());
        ServiceContainerUtil.replaceService(this.getProject(), PhpInterpreterProvider.class, this.interpreterProvider, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        DdevSettingsState.getInstance(this.getProject()).autoConfigurePhpInterpreter = true;
        super.tearDown();
    }

    @Test
    void registersInterpreterForExistingComposeFile() {
        this.mockComposeFile(true);

        new ConfigurationProviderImpl(this.getProject()).configure(description("acol", "8.1"));

        Mockito.verify(this.interpreterProvider).registerInterpreter(new DdevInterpreterConfig("acol", "php8.1", COMPOSE_FILE));
    }

    @Test
    void disabledSettingSkipsRegistration() {
        this.mockComposeFile(true);
        DdevSettingsState.getInstance(this.getProject()).autoConfigurePhpInterpreter = false;

        new ConfigurationProviderImpl(this.getProject()).configure(description("acol", "8.1"));

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void disposedProjectSkipsRegistration() {
        final Project project = Mockito.mock(Project.class);
        Mockito.when(project.isDisposed()).thenReturn(true);

        new ConfigurationProviderImpl(project).configure(description("acol", "8.1"));

        Mockito.verify(project, Mockito.never()).getService(Mockito.any());
    }

    @Test
    void missingNameSkipsRegistration() {
        this.mockComposeFile(true);

        new ConfigurationProviderImpl(this.getProject()).configure(description(null, "8.1"));

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void missingPhpVersionSkipsRegistration() {
        this.mockComposeFile(true);

        new ConfigurationProviderImpl(this.getProject()).configure(description("acol", null));

        Mockito.verifyNoInteractions(this.composeFileLoader, this.interpreterProvider);
    }

    @Test
    void missingComposeFileSkipsRegistration() {
        Mockito.when(this.composeFileLoader.load()).thenReturn(null);

        new ConfigurationProviderImpl(this.getProject()).configure(description("acol", "8.1"));

        Mockito.verifyNoInteractions(this.interpreterProvider);
    }

    @Test
    void deletedComposeFileSkipsRegistration() {
        this.mockComposeFile(false);

        new ConfigurationProviderImpl(this.getProject()).configure(description("acol", "8.1"));

        Mockito.verifyNoInteractions(this.interpreterProvider);
    }

    private void mockComposeFile(boolean exists) {
        final VirtualFile composeFile = Mockito.mock(VirtualFile.class);
        Mockito.when(composeFile.exists()).thenReturn(exists);
        Mockito.when(composeFile.getPath()).thenReturn(COMPOSE_FILE);
        Mockito.when(this.composeFileLoader.load()).thenReturn(composeFile);
    }

    private static Description description(String name, String phpVersion) {
        return Description.builder().name(name).phpVersion(phpVersion).build();
    }
}
