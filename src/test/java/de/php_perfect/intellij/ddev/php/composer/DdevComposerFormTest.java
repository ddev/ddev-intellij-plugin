package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.composer.execution.ComposerExecution;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DdevComposerFormTest extends BasePlatformTestCase {
    private DdevComposerForm form;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.form = new DdevComposerForm(this.getProject(), this.getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void showsDescriptionPanel() {
        Assertions.assertEquals(1, this.form.getComponent().getComponentCount());
        Assertions.assertNull(this.form.validate());
    }

    @Test
    void resetAcceptsDdevExecution() {
        final DdevComposerExecution execution = new DdevComposerExecution();

        Assertions.assertTrue(this.form.reset(execution));
        Assertions.assertSame(execution, this.form.getExecution());
    }

    @Test
    void resetRejectsOtherExecution() {
        final ComposerExecution initialExecution = this.form.getExecution();

        Assertions.assertFalse(this.form.reset(Mockito.mock(ComposerExecution.class)));
        Assertions.assertSame(initialExecution, this.form.getExecution());
    }

    @Test
    void applyKeepsExecution() {
        final ComposerExecution execution = this.form.getExecution();

        this.form.apply();

        Assertions.assertSame(execution, this.form.getExecution());
    }

    @Test
    void onlyOtherExecutionsAreModified() {
        Assertions.assertFalse(this.form.isModified(new DdevComposerExecution()));
        Assertions.assertTrue(this.form.isModified(Mockito.mock(ComposerExecution.class)));
    }
}
