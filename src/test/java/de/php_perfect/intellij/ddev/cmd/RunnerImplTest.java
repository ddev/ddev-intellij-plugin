package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

final class RunnerImplTest extends BasePlatformTestCase {
    private RunnerImpl runner;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.runner = new RunnerImpl(getProject());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            Disposer.dispose(this.runner);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void runsCommandAndCallsCompletionHandler() throws InterruptedException {
        final GeneralCommandLine commandLine = new GeneralCommandLine(ProcessExecutorImplTest.javaBinary(), "-version")
                .withWorkDirectory(System.getProperty("java.io.tmpdir"));
        final CountDownLatch completed = new CountDownLatch(1);
        final RunContentManager contentManager = Mockito.mock(RunContentManager.class);
        ServiceContainerUtil.replaceService(getProject(), RunContentManager.class, contentManager, getTestRootDisposable());

        this.runner.run(commandLine, "Java Version", completed::countDown);

        Assertions.assertTrue(completed.await(60, TimeUnit.SECONDS));

        final ArgumentCaptor<RunContentDescriptor> descriptor = ArgumentCaptor.forClass(RunContentDescriptor.class);
        Mockito.verify(contentManager).showRunContent(Mockito.any(), descriptor.capture());
        Assertions.assertEquals("Java Version", descriptor.getValue().getDisplayName());
        ApplicationManager.getApplication().invokeAndWait(() -> Disposer.dispose(descriptor.getValue()));
    }

    @Test
    void passesTheStartedProcessToTheConsumer() throws InterruptedException {
        final GeneralCommandLine commandLine = new GeneralCommandLine(ProcessExecutorImplTest.javaBinary(), "-version")
                .withWorkDirectory(System.getProperty("java.io.tmpdir"));
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicReference<ProcessHandler> started = new AtomicReference<>();
        final RunContentManager contentManager = Mockito.mock(RunContentManager.class);
        ServiceContainerUtil.replaceService(getProject(), RunContentManager.class, contentManager, getTestRootDisposable());

        this.runner.run(commandLine, "Java Version", completed::countDown, started::set);

        Assertions.assertTrue(completed.await(60, TimeUnit.SECONDS));
        Assertions.assertNotNull(started.get());

        final ArgumentCaptor<RunContentDescriptor> descriptor = ArgumentCaptor.forClass(RunContentDescriptor.class);
        Mockito.verify(contentManager).showRunContent(Mockito.any(), descriptor.capture());
        Assertions.assertSame(started.get(), descriptor.getValue().getProcessHandler());
        ApplicationManager.getApplication().invokeAndWait(() -> Disposer.dispose(descriptor.getValue()));
    }

    @Test
    void runWithoutCompletionHandlerDelegates() {
        final GeneralCommandLine commandLine = new GeneralCommandLine("ddev", "start");
        final RunnerImpl spy = Mockito.spy(this.runner);
        Mockito.doNothing().when(spy).run(Mockito.any(), Mockito.anyString(), Mockito.isNull());

        spy.run(commandLine, "DDEV Start");

        Mockito.verify(spy).run(commandLine, "DDEV Start", null);
    }

    @Test
    void unstartableCommandIsLogged() throws InterruptedException {
        final String missingBinary = Path.of(System.getProperty("java.io.tmpdir"), "missing-ddev-" + System.nanoTime()).toString();
        final GeneralCommandLine commandLine = new GeneralCommandLine(missingBinary, "start")
                .withWorkDirectory(System.getProperty("java.io.tmpdir"));
        final CountDownLatch logged = new CountDownLatch(1);
        final AtomicReference<String> message = new AtomicReference<>();

        LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
            @Override
            public boolean processWarn(@NotNull String category, @NotNull String warning, @Nullable Throwable t) {
                if (warning.startsWith("An error occurred running")) {
                    message.set(warning);
                    logged.countDown();
                    return false;
                }
                return super.processWarn(category, warning, t);
            }
        }, () -> {
            this.runner.run(commandLine, "Missing");
            Assertions.assertTrue(logged.await(30, TimeUnit.SECONDS));
        });

        Assertions.assertTrue(message.get().contains(missingBinary));
    }
}
