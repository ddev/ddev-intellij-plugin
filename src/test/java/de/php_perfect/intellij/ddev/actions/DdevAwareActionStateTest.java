package de.php_perfect.intellij.ddev.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.testFramework.TestActionEvent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.cmd.Description.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.net.URI;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class DdevAwareActionStateTest extends BasePlatformTestCase {
    private static final Named<Supplier<DdevAwareAction>> START = Named.of("start", DdevStartAction::new);
    private static final Named<Supplier<DdevAwareAction>> RESTART = Named.of("restart", DdevRestartAction::new);
    private static final Named<Supplier<DdevAwareAction>> STOP = Named.of("stop", DdevStopAction::new);
    private static final Named<Supplier<DdevAwareAction>> POWER_OFF = Named.of("power off", DdevPowerOffAction::new);
    private static final Named<Supplier<DdevAwareAction>> DELETE = Named.of("delete", DdevDeleteAction::new);
    private static final Named<Supplier<DdevAwareAction>> SHARE = Named.of("share", DdevShareAction::new);
    private static final Named<Supplier<DdevAwareAction>> CONFIG = Named.of("config", DdevConfigAction::new);
    private static final Named<Supplier<DdevAwareAction>> TERMINAL = Named.of("terminal", DdevPredefinedTerminalAction::new);
    private static final Named<Supplier<DdevAwareAction>> OPEN_SERVICE = Named.of("open service",
            () -> new OpenServiceAction(URI.create("https://test.ddev.site"), "Open", null, null));

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

    static Stream<Arguments> states() {
        return Stream.of(
                Arguments.of(START, false, false, null, false),
                Arguments.of(START, true, false, null, false),
                Arguments.of(START, true, true, null, true),
                Arguments.of(START, true, true, Status.RUNNING, false),
                Arguments.of(START, true, true, Status.STARTING, false),
                Arguments.of(START, true, true, Status.STOPPED, true),

                Arguments.of(RESTART, false, true, Status.RUNNING, false),
                Arguments.of(RESTART, true, false, Status.RUNNING, false),
                Arguments.of(RESTART, true, true, null, true),
                Arguments.of(RESTART, true, true, Status.RUNNING, true),
                Arguments.of(RESTART, true, true, Status.STOPPED, false),

                Arguments.of(STOP, false, true, Status.RUNNING, false),
                Arguments.of(STOP, true, false, Status.RUNNING, false),
                Arguments.of(STOP, true, true, null, true),
                Arguments.of(STOP, true, true, Status.RUNNING, true),
                Arguments.of(STOP, true, true, Status.PAUSED, false),

                Arguments.of(SHARE, false, true, Status.RUNNING, false),
                Arguments.of(SHARE, true, true, null, false),
                Arguments.of(SHARE, true, false, Status.RUNNING, true),
                Arguments.of(SHARE, true, true, Status.STOPPED, false),

                Arguments.of(CONFIG, false, false, null, false),
                Arguments.of(CONFIG, true, false, null, true),
                Arguments.of(CONFIG, true, true, null, false),

                Arguments.of(DELETE, false, true, null, false),
                Arguments.of(DELETE, true, false, null, false),
                Arguments.of(DELETE, true, true, null, true),

                Arguments.of(POWER_OFF, false, true, null, false),
                Arguments.of(POWER_OFF, true, false, null, false),
                Arguments.of(POWER_OFF, true, true, null, true),

                Arguments.of(TERMINAL, false, true, Status.RUNNING, false),
                Arguments.of(TERMINAL, true, false, Status.RUNNING, false),
                Arguments.of(TERMINAL, true, true, null, false),
                Arguments.of(TERMINAL, true, true, Status.RUNNING, true),
                Arguments.of(TERMINAL, true, true, Status.STOPPED, false),

                Arguments.of(OPEN_SERVICE, false, true, Status.RUNNING, false),
                Arguments.of(OPEN_SERVICE, true, false, Status.RUNNING, false),
                Arguments.of(OPEN_SERVICE, true, true, null, false),
                Arguments.of(OPEN_SERVICE, true, true, Status.RUNNING, true),
                Arguments.of(OPEN_SERVICE, true, true, Status.UNHEALTHY, false)
        );
    }

    @ParameterizedTest(name = "{0}: available={1}, configured={2}, status={3}")
    @MethodSource("states")
    void enabledStateFollowsDdevState(Supplier<DdevAwareAction> actionSupplier, boolean available, boolean configured,
                                      Status status, boolean expectedEnabled) {
        DdevStateStub.install(getProject(), getTestRootDisposable(), available, configured, status);

        final DdevAwareAction action = actionSupplier.get();
        final AnActionEvent event = TestActionEvent.createTestEvent(action, SimpleDataContext.getProjectContext(getProject()));
        event.getPresentation().setEnabled(!expectedEnabled);

        action.update(event);

        Assertions.assertEquals(expectedEnabled, event.getPresentation().isEnabled());
    }
}
