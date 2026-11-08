package de.php_perfect.intellij.ddev.status_bar;

import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

final class DdevStatusBarWidgetFactoryImplTest extends BasePlatformTestCase {
    private final DdevStatusBarWidgetFactoryImpl factory = new DdevStatusBarWidgetFactoryImpl();

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void factoryIsNamedAfterWidget() {
        Assertions.assertEquals(DdevStatusBarWidgetImpl.WIDGET_ID, this.factory.getId());
        Assertions.assertEquals("DDEV Status", this.factory.getDisplayName());
    }

    @Test
    void canBeEnabledOnProjectStatusBar() {
        final StatusBar statusBar = Mockito.mock(StatusBar.class);
        Mockito.when(statusBar.getProject()).thenReturn(this.getProject());

        Assertions.assertTrue(this.factory.canBeEnabledOn(statusBar));
    }

    @Test
    void cannotBeEnabledOnStatusBarWithoutProject() {
        final StatusBar statusBar = Mockito.mock(StatusBar.class);
        Mockito.when(statusBar.getProject()).thenReturn(null);

        Assertions.assertFalse(this.factory.canBeEnabledOn(statusBar));
    }

    @Test
    void createsDdevWidgetForProject() {
        final StatusBarWidget widget = this.factory.createWidget(this.getProject());
        Disposer.register(this.getTestRootDisposable(), widget);

        Assertions.assertInstanceOf(DdevStatusBarWidgetImpl.class, widget);
        Assertions.assertEquals(DdevStatusBarWidgetImpl.WIDGET_ID, widget.ID());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
