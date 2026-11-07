package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.composer.execution.ComposerExecution;
import com.jetbrains.php.composer.execution.ComposerExecutionProvider;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import org.jdom.Element;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DdevComposerExecutionProviderTest extends BasePlatformTestCase {
    private final DdevComposerExecutionProvider provider = new DdevComposerExecutionProvider();

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
    void presentableNameComesFromBundle() {
        Assertions.assertEquals(DdevIntegrationBundle.message("composer.execution.provider.ddev.name"), this.provider.getPresentableName());
    }

    @Test
    void recognisesDdevExecutions() {
        Assertions.assertTrue(this.provider.isMyExecution(new DdevComposerExecution()));
        Assertions.assertFalse(this.provider.isMyExecution(Mockito.mock(ComposerExecution.class)));
    }

    @Test
    void createsDdevForm() {
        final ComposerExecutionProvider.Form form = this.provider.createForm(this.getProject(), this.getTestRootDisposable());

        Assertions.assertInstanceOf(DdevComposerForm.class, form);
    }

    @Test
    void loadsSavedExecution() {
        Assertions.assertEquals(new DdevComposerExecution(), this.provider.loadExecution(new DdevComposerExecution().save()));
        Assertions.assertNull(this.provider.loadExecution(new Element("local")));
    }
}
