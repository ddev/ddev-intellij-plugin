package de.php_perfect.intellij.ddev.status_bar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListPopupStep;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.TextPanel;
import com.intellij.testFramework.EdtTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.ComponentUtil;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.icons.DdevIntegrationIcons;
import de.php_perfect.intellij.ddev.service_actions.ServiceActionManager;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.tutorial.GotItTutorial;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class DdevStatusBarWidgetImplTest extends BasePlatformTestCase {
    private State managerState;
    private DdevStatusBarWidgetImpl widget;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.managerState = Mockito.mock(State.class);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.when(stateManager.getState()).thenReturn(this.managerState);
        ServiceContainerUtil.replaceService(this.getProject(), DdevStateManager.class, stateManager, this.getTestRootDisposable());

        this.widget = this.createWidget();
    }

    @Test
    void widgetIdMatchesFactory() {
        Assertions.assertEquals(DdevStatusBarWidgetImpl.WIDGET_ID, this.widget.ID());
    }

    @Test
    void hiddenWhileDdevIsUnavailable() {
        Mockito.when(this.managerState.isAvailable()).thenReturn(false);
        Mockito.when(this.managerState.isConfigured()).thenReturn(true);

        Assertions.assertFalse(this.render().isVisible());
    }

    @Test
    void hiddenWithoutDdevProjectConfig() {
        Mockito.when(this.managerState.isAvailable()).thenReturn(true);
        Mockito.when(this.managerState.isConfigured()).thenReturn(false);

        Assertions.assertFalse(this.render().isVisible());
    }

    @Test
    void showsUnknownStatusWithoutDescription() {
        this.stubManagerState(null);

        final TextPanel.WithIconAndArrows component = this.render();

        Assertions.assertTrue(component.isVisible());
        Assertions.assertTrue(component.isEnabled());
        Assertions.assertEquals("Unknown", component.getText());
        Assertions.assertEquals("DDEV Status and Services", component.getToolTipText());
        Assertions.assertSame(DdevIntegrationIcons.DdevLogoMono, component.getIcon());
    }

    @Test
    void showsUnknownStatusForDescriptionWithoutStatus() {
        this.stubManagerState(Description.builder().name("acol").build());

        Assertions.assertEquals("Unknown", this.render().getText());
    }

    @Test
    void showsProjectStatus() {
        final Map<Description.Status, String> expectedTexts = Map.of(
                Description.Status.RUNNING, "Running",
                Description.Status.STARTING, "Starting",
                Description.Status.STOPPED, "Stopped",
                Description.Status.DIR_MISSING, "Project directory missing",
                Description.Status.CONFIG_MISSING, ".ddev/config.yaml missing",
                Description.Status.PAUSED, "Paused",
                Description.Status.UNHEALTHY, "Unhealthy"
        );
        Assertions.assertEquals(Description.Status.values().length, expectedTexts.size());

        expectedTexts.forEach((status, text) -> {
            this.stubManagerState(Description.builder().status(status).build());

            Assertions.assertEquals(text, this.render().getText(), status.name());
        });
    }

    @Test
    void publishedStateTakesPrecedenceOverStateManager() {
        Mockito.when(this.managerState.isAvailable()).thenReturn(false);
        this.widget.install(this.statusBarOf(this.getProject()));

        this.publish(this.availableState(Description.builder().status(Description.Status.PAUSED).build()));

        Assertions.assertEquals("Paused", this.render().getText());
    }

    @Test
    void statusBarWithoutProjectReceivesStateUpdates() {
        Mockito.when(this.managerState.isAvailable()).thenReturn(false);
        this.widget.install(this.statusBarOf(null));

        this.publish(this.availableState(Description.builder().status(Description.Status.RUNNING).build()));

        Assertions.assertEquals("Running", this.render().getText());
    }

    @Test
    void statusBarOfAnotherProjectIsIgnored() {
        Mockito.when(this.managerState.isAvailable()).thenReturn(false);
        this.widget.install(this.statusBarOf(Mockito.mock(Project.class)));

        this.publish(this.availableState(Description.builder().status(Description.Status.RUNNING).build()));

        Assertions.assertFalse(this.render().isVisible());
    }

    @Test
    void tutorialIsShownOnceWidgetIsVisible() throws InterruptedException {
        final CountDownLatch shown = new CountDownLatch(1);
        final GotItTutorial tutorial = Mockito.mock(GotItTutorial.class);
        Mockito.doAnswer(invocation -> {
            shown.countDown();
            return null;
        }).when(tutorial).showStatusBarTutorial(Mockito.any(), Mockito.any());
        ServiceContainerUtil.registerOrReplaceServiceInstance(
                ApplicationManager.getApplication(), GotItTutorial.class, tutorial, this.getTestRootDisposable());

        final JComponent component = this.widget.getComponent();
        ComponentUtil.forceMarkAsShowing(component, true);
        this.widget.install(this.statusBarOf(this.getProject()));

        Assertions.assertTrue(shown.await(10, TimeUnit.SECONDS));
        Mockito.verify(tutorial).showStatusBarTutorial(component, this.widget);
    }

    @Test
    void createInstanceBuildsSeparateWidget() {
        final StatusBarWidget copy = this.widget.createInstance(this.getProject());
        Disposer.register(this.getTestRootDisposable(), copy);

        Assertions.assertInstanceOf(DdevStatusBarWidgetImpl.class, copy);
        Assertions.assertNotSame(this.widget, copy);
    }

    @Test
    void popupListsServiceActions() {
        final ServiceActionManager serviceActionManager = Mockito.mock(ServiceActionManager.class);
        Mockito.when(serviceActionManager.getServiceActions()).thenReturn(new AnAction[]{new NamedAction("Open phpMyAdmin")});
        ServiceContainerUtil.replaceService(this.getProject(), ServiceActionManager.class, serviceActionManager, this.getTestRootDisposable());

        final List<String> texts = EdtTestUtil.runInEdtAndGet(() -> {
            final ListPopup popup = this.widget.createPopup(SimpleDataContext.getProjectContext(this.getProject()));
            try {
                @SuppressWarnings("unchecked") final ListPopupStep<Object> step = (ListPopupStep<Object>) popup.getListStep();
                return step.getValues().stream().map(step::getTextFor).toList();
            } finally {
                Disposer.dispose(popup);
            }
        });

        Assertions.assertEquals(List.of("Open phpMyAdmin"), texts);
    }

    private TextPanel.WithIconAndArrows render() {
        final TextPanel.WithIconAndArrows component = (TextPanel.WithIconAndArrows) this.widget.getComponent();
        EdtTestUtil.runInEdtAndWait(() -> this.widget.updateInTests(true));
        return component;
    }

    private DdevStatusBarWidgetImpl createWidget() {
        final DdevStatusBarWidgetImpl newWidget = new DdevStatusBarWidgetImpl(this.getProject());
        Disposer.register(this.getTestRootDisposable(), newWidget);
        return newWidget;
    }

    private void stubManagerState(Description description) {
        Mockito.when(this.managerState.isAvailable()).thenReturn(true);
        Mockito.when(this.managerState.isConfigured()).thenReturn(true);
        Mockito.when(this.managerState.getDescription()).thenReturn(description);
    }

    private State availableState(Description description) {
        final State state = Mockito.mock(State.class);
        Mockito.when(state.isAvailable()).thenReturn(true);
        Mockito.when(state.isConfigured()).thenReturn(true);
        Mockito.when(state.getDescription()).thenReturn(description);
        return state;
    }

    private StatusBar statusBarOf(Project project) {
        final StatusBar statusBar = Mockito.mock(StatusBar.class);
        Mockito.when(statusBar.getProject()).thenReturn(project);
        return statusBar;
    }

    private void publish(State state) {
        this.getProject().getMessageBus().syncPublisher(StateChangedListener.DDEV_CHANGED).onDdevChanged(state);
    }

    private static final class NamedAction extends DumbAwareAction {
        private NamedAction(String text) {
            super(text);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            // Only the action text matters to these tests.
        }
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            final Key<?> stateKey = Key.findKeyByName("DdevIntegration.State");
            if (stateKey != null) {
                this.getProject().putUserData(stateKey, null);
            }
        } finally {
            super.tearDown();
        }
    }
}
