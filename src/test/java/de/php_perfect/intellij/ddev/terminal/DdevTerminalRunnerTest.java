package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;
import com.pty4j.unix.UnixPtyProcess;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.MockDdevConfigLoader;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

final class DdevTerminalRunnerTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void createProcessNotExistentDdev() throws NoSuchFieldException, IllegalAccessException {
        Project project = getProject();
        DdevTerminalRunner ddevTerminalRunner = new DdevTerminalRunner(project);

        State state = DdevStateManager.getInstance(project).getState();

        Field field = state.getClass().getDeclaredField("ddevBinary");
        field.setAccessible(true);
        field.set(state, null);

        Assertions.assertThrowsExactly(ExecutionException.class, ddevTerminalRunner::createSessionProcess);
    }

    @Test
    void terminalIsNotPersistent() {
        Project project = getProject();
        DdevTerminalRunner ddevTerminalRunner = new DdevTerminalRunner(project);

        Assertions.assertFalse(ddevTerminalRunner.isTerminalSessionPersistent());
    }

    @Test
    void connectorResizesLiveProcess() {
        final PtyProcess process = mockProcess(PtyProcess.class);
        Mockito.when(process.isAlive()).thenReturn(true);

        final TtyConnector connector = new DdevTerminalRunner(getProject()).createSessionConnector(process);
        connector.resize(new TermSize(120, 40));

        Assertions.assertEquals("Local", connector.getName());
        Mockito.verify(process).setWinSize(Mockito.argThat((WinSize size) -> size.getColumns() == 120 && size.getRows() == 40));
    }

    @Test
    void connectorSkipsResizeOfFinishedProcess() {
        final PtyProcess process = mockProcess(PtyProcess.class);
        Mockito.when(process.isAlive()).thenReturn(false);

        new DdevTerminalRunner(getProject()).createSessionConnector(process).resize(new TermSize(120, 40));

        Mockito.verify(process, Mockito.never()).setWinSize(Mockito.any());
    }

    @Test
    void connectorDestroysNonUnixProcessOnClose() {
        final PtyProcess process = mockProcess(PtyProcess.class);

        new DdevTerminalRunner(getProject()).createSessionConnector(process).close();

        Mockito.verify(process).destroy();
    }

    @Test
    void connectorDestroysUnixProcessThatSurvivesHangup() throws InterruptedException {
        final UnixPtyProcess process = mockProcess(UnixPtyProcess.class);
        final CountDownLatch destroyed = new CountDownLatch(1);
        Mockito.when(process.isAlive()).thenReturn(true);
        Mockito.doAnswer(invocation -> {
            destroyed.countDown();
            return null;
        }).when(process).destroy();

        new DdevTerminalRunner(getProject()).createSessionConnector(process).close();

        Mockito.verify(process).hangup();
        Assertions.assertTrue(destroyed.await(5, TimeUnit.SECONDS));
    }

    private static <T extends PtyProcess> T mockProcess(Class<T> type) {
        final T process = Mockito.mock(type);
        Mockito.when(process.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        Mockito.when(process.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        return process;
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        final MockDdevConfigLoader ddevConfigLoader = (MockDdevConfigLoader) DdevConfigLoader.getInstance(this.getProject());
        ddevConfigLoader.setExists(false);

        DdevStateManager.getInstance(this.getProject()).resetState();

        super.tearDown();
    }
}
