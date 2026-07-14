package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TestDialog;
import com.intellij.openapi.ui.TestDialogManager;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class DdevRunActionTest extends BasePlatformTestCase {
    private DdevRunner runner;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.runner = Mockito.mock(DdevRunner.class);
        ServiceContainerUtil.replaceService(ApplicationManager.getApplication(), DdevRunner.class, this.runner, getTestRootDisposable());
        TestDialogManager.setTestDialog(TestDialog.YES);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            TestDialogManager.setTestDialog(TestDialog.DEFAULT);
        } finally {
            super.tearDown();
        }
    }

    static Stream<Arguments> runActions() {
        return Stream.of(
                runAction("start", DdevStartAction::new, DdevRunner::start),
                runAction("restart", DdevRestartAction::new, DdevRunner::restart),
                runAction("stop", DdevStopAction::new, DdevRunner::stop),
                runAction("power off", DdevPowerOffAction::new, DdevRunner::powerOff),
                runAction("delete", DdevDeleteAction::new, DdevRunner::delete),
                runAction("share", DdevShareAction::new, DdevRunner::share),
                runAction("config", DdevConfigAction::new, DdevRunner::config)
        );
    }

    private static Arguments runAction(String name, Supplier<DdevRunAction> action, BiConsumer<DdevRunner, Project> command) {
        return Arguments.of(Named.of(name, action), command);
    }

    @ParameterizedTest
    @MethodSource("runActions")
    void actionRunsDdevCommandForProject(Supplier<DdevRunAction> actionSupplier, BiConsumer<DdevRunner, Project> command) {
        final DdevRunAction action = actionSupplier.get();

        action.actionPerformed(event(action, SimpleDataContext.getProjectContext(getProject())));

        command.accept(Mockito.verify(this.runner), getProject());
    }

    @Test
    void declinedDeleteRunsNothing() {
        TestDialogManager.setTestDialog(TestDialog.NO);
        final DdevRunAction action = new DdevDeleteAction();

        action.actionPerformed(event(action, SimpleDataContext.getProjectContext(getProject())));

        Mockito.verifyNoInteractions(this.runner);
    }

    @ParameterizedTest
    @MethodSource("runActions")
    void actionWithoutProjectRunsNothing(Supplier<DdevRunAction> actionSupplier, BiConsumer<DdevRunner, Project> ignored) {
        final DdevRunAction action = actionSupplier.get();

        action.actionPerformed(event(action, DataContext.EMPTY_CONTEXT));

        Mockito.verifyNoInteractions(this.runner);
    }

    @ParameterizedTest
    @MethodSource("runActions")
    void actionWithoutProjectIsDisabled(Supplier<DdevRunAction> actionSupplier, BiConsumer<DdevRunner, Project> ignored) {
        final DdevRunAction action = actionSupplier.get();
        final AnActionEvent event = event(action, DataContext.EMPTY_CONTEXT);

        action.update(event);

        Assertions.assertFalse(event.getPresentation().isEnabled());
    }

    @ParameterizedTest
    @MethodSource("runActions")
    void actionIsDisabledWhenStateIsUnreadable(Supplier<DdevRunAction> actionSupplier, BiConsumer<DdevRunner, Project> ignored) {
        final DdevStateManager manager = Mockito.mock(DdevStateManager.class);
        Mockito.when(manager.getState()).thenThrow(new IllegalStateException("state unavailable"));
        DdevStateStub.install(getProject(), getTestRootDisposable(), manager);

        final DdevRunAction action = actionSupplier.get();
        final AnActionEvent event = event(action, SimpleDataContext.getProjectContext(getProject()));

        action.update(event);

        Assertions.assertFalse(event.getPresentation().isEnabled());
    }

    @ParameterizedTest
    @MethodSource("runActions")
    void actionUpdatesInBackground(Supplier<DdevRunAction> actionSupplier, BiConsumer<DdevRunner, Project> ignored) {
        Assertions.assertEquals(ActionUpdateThread.BGT, actionSupplier.get().getActionUpdateThread());
    }

    private static AnActionEvent event(DdevRunAction action, DataContext dataContext) {
        return TestActionEvent.createTestEvent(action, dataContext);
    }
}
