package de.php_perfect.intellij.ddev.php;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class AutoConfigurePhpInterpreterListenerTest extends BasePlatformTestCase {
    private ConfigurationProvider configurationProvider;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.configurationProvider = Mockito.mock(ConfigurationProvider.class);
        ServiceContainerUtil.replaceService(this.getProject(), ConfigurationProvider.class, this.configurationProvider, this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void configuresInterpreterForDescription() {
        final Description description = Description.builder().name("acol").phpVersion("8.1").build();

        new AutoConfigurePhpInterpreterListener(this.getProject()).onDescriptionChanged(description);

        Mockito.verify(this.configurationProvider).configure(description);
    }

    @Test
    void missingDescriptionIsIgnored() {
        new AutoConfigurePhpInterpreterListener(this.getProject()).onDescriptionChanged(null);

        Mockito.verifyNoInteractions(this.configurationProvider);
    }
}
