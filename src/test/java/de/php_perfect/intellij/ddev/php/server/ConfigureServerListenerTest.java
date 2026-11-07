package de.php_perfect.intellij.ddev.php.server;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.net.URI;
import java.util.Objects;

final class ConfigureServerListenerTest extends BasePlatformTestCase {
    private ServerConfigManager serverConfigManager;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.serverConfigManager = Mockito.mock(ServerConfigManager.class);
        ServiceContainerUtil.replaceService(this.getProject(), ServerConfigManager.class, this.serverConfigManager, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void configuresServerForPrimaryUrl() {
        new ConfigureServerListener(this.getProject()).onDescriptionChanged(description("https://acol.ddev.site"));

        final String basePath = Objects.requireNonNull(this.getProject().getBasePath());
        Mockito.verify(this.serverConfigManager).configure(new ServerConfig(basePath, "/var/www/html", URI.create("https://acol.ddev.site")));
    }

    @Test
    void missingDescriptionIsIgnored() {
        new ConfigureServerListener(this.getProject()).onDescriptionChanged(null);

        Mockito.verifyNoInteractions(this.serverConfigManager);
    }

    @Test
    void projectWithoutBasePathIsIgnored() {
        final Project project = Mockito.mock(Project.class);

        new ConfigureServerListener(project).onDescriptionChanged(description("https://acol.ddev.site"));

        Mockito.verify(project, Mockito.never()).getService(Mockito.any());
    }

    @Test
    void missingPrimaryUrlIsIgnored() {
        new ConfigureServerListener(this.getProject()).onDescriptionChanged(description(null));

        Mockito.verifyNoInteractions(this.serverConfigManager);
    }

    @Test
    void malformedPrimaryUrlIsIgnored() {
        new ConfigureServerListener(this.getProject()).onDescriptionChanged(description("https://acol ddev.site"));

        Mockito.verifyNoInteractions(this.serverConfigManager);
    }

    private static Description description(String primaryUrl) {
        return Description.builder().name("acol").primaryUrl(primaryUrl).build();
    }
}
