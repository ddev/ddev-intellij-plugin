package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.ExtensionTestUtil;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.DdevConfigArgumentProvider;
import de.php_perfect.intellij.ddev.state.DdevConfigLoader;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

final class DdevRunnerImplTest extends BasePlatformTestCase {
    private static final ExtensionPointName<DdevConfigArgumentProvider> CONFIG_ARGUMENT_PROVIDER_EP = ExtensionPointName.create("de.php_perfect.intellij.ddev.ddevConfigArgumentProvider");

    private Runner runner;
    private DdevStateManager stateManager;
    private DdevConfigLoader configLoader;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.runner = Mockito.mock(Runner.class);
        this.stateManager = Mockito.mock(DdevStateManager.class);
        this.configLoader = Mockito.mock(DdevConfigLoader.class);

        final State state = Mockito.mock(State.class);
        Mockito.when(state.getDdevBinary()).thenReturn("ddev");
        Mockito.when(this.stateManager.getState()).thenReturn(state);

        final Project project = getProject();
        ServiceContainerUtil.replaceService(project, Runner.class, this.runner, getTestRootDisposable());
        ServiceContainerUtil.replaceService(project, DdevStateManager.class, this.stateManager, getTestRootDisposable());
        ServiceContainerUtil.replaceService(project, DdevConfigLoader.class, this.configLoader, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void start() throws InterruptedException {
        this.assertRunsAndRefreshesDescription(ddevRunner -> ddevRunner.start(getProject()), "ddev start", "DDEV Start");
    }

    @Test
    void restart() throws InterruptedException {
        this.assertRunsAndRefreshesDescription(ddevRunner -> ddevRunner.restart(getProject()), "ddev restart", "DDEV Restart");
    }

    @Test
    void stop() throws InterruptedException {
        this.assertRunsAndRefreshesDescription(ddevRunner -> ddevRunner.stop(getProject()), "ddev stop", "DDEV Stop");
    }

    @Test
    void powerOff() throws InterruptedException {
        this.assertRunsAndRefreshesDescription(ddevRunner -> ddevRunner.powerOff(getProject()), "ddev poweroff", "DDEV Power Off");
    }

    @Test
    void delete() throws InterruptedException {
        final ArgumentCaptor<GeneralCommandLine> commandLine = ArgumentCaptor.forClass(GeneralCommandLine.class);
        final ArgumentCaptor<Runnable> afterSuccess = ArgumentCaptor.forClass(Runnable.class);
        final CountDownLatch updated = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(this.stateManager).updateDescription();

        new DdevRunnerImpl().delete(getProject());

        Mockito.verify(this.runner).runOnSuccess(commandLine.capture(), Mockito.eq("DDEV Delete"), afterSuccess.capture());
        Assertions.assertEquals("ddev delete --yes", commandLine.getValue().getCommandLineString());

        afterSuccess.getValue().run();

        Assertions.assertTrue(updated.await(10, TimeUnit.SECONDS));
    }

    @Test
    void share() {
        final ArgumentCaptor<GeneralCommandLine> commandLine = ArgumentCaptor.forClass(GeneralCommandLine.class);

        new DdevRunnerImpl().share(getProject());

        Mockito.verify(this.runner).run(commandLine.capture(), Mockito.eq("DDEV Share"), Mockito.any(Runnable.class), Mockito.any());
        Assertions.assertEquals("ddev share", commandLine.getValue().getCommandLineString());
        Assertions.assertEquals("true", commandLine.getValue().getEnvironment().get("DDEV_NONINTERACTIVE"));
    }

    @Test
    void configPassesProviderArguments() throws InterruptedException {
        ExtensionTestUtil.maskExtensions(CONFIG_ARGUMENT_PROVIDER_EP, List.of(project -> List.of("--php-version=8.3")), getTestRootDisposable());

        final Runnable afterCompletion = this.runConfig("ddev config --auto --php-version=8.3");
        this.assertUpdatesConfiguration(afterCompletion);
    }

    @Test
    void configOpensExistingConfigFile() throws InterruptedException {
        ExtensionTestUtil.maskExtensions(CONFIG_ARGUMENT_PROVIDER_EP, List.of(), getTestRootDisposable());
        final VirtualFile config = myFixture.addFileToProject(".ddev/config.yaml", "name: test\n").getVirtualFile();
        Mockito.when(this.configLoader.load()).thenReturn(config);

        final Runnable afterCompletion = this.runConfig("ddev config --auto");
        this.assertUpdatesConfiguration(afterCompletion);

        Assertions.assertTrue(FileEditorManager.getInstance(getProject()).isFileOpen(config));
    }

    @Test
    void configSkipsDeletedConfigFile() throws InterruptedException {
        ExtensionTestUtil.maskExtensions(CONFIG_ARGUMENT_PROVIDER_EP, List.of(), getTestRootDisposable());
        final VirtualFile config = Mockito.mock(VirtualFile.class);
        Mockito.when(config.exists()).thenReturn(false);
        Mockito.when(this.configLoader.load()).thenReturn(config);

        final Runnable afterCompletion = this.runConfig("ddev config --auto");
        this.assertUpdatesConfiguration(afterCompletion);

        Mockito.verify(config).exists();
    }

    @Test
    void configWithoutConfigFile() throws InterruptedException {
        ExtensionTestUtil.maskExtensions(CONFIG_ARGUMENT_PROVIDER_EP, List.of(), getTestRootDisposable());
        Mockito.when(this.configLoader.load()).thenReturn(null);

        final Runnable afterCompletion = this.runConfig("ddev config --auto");
        this.assertUpdatesConfiguration(afterCompletion);

        Mockito.verify(this.configLoader).load();
    }

    @Test
    void lookupReturnsRegisteredService() {
        Assertions.assertInstanceOf(DdevRunnerImpl.class, DdevRunner.getInstance());
    }

    private Runnable runConfig(String expectedCommandLine) {
        final ArgumentCaptor<GeneralCommandLine> commandLine = ArgumentCaptor.forClass(GeneralCommandLine.class);
        final ArgumentCaptor<Runnable> afterCompletion = ArgumentCaptor.forClass(Runnable.class);

        new DdevRunnerImpl().config(getProject());

        Mockito.verify(this.runner).run(commandLine.capture(), Mockito.eq("DDEV Config"), afterCompletion.capture());
        Assertions.assertEquals(expectedCommandLine, commandLine.getValue().getCommandLineString());

        return afterCompletion.getValue();
    }

    private void assertUpdatesConfiguration(Runnable afterCompletion) throws InterruptedException {
        final CountDownLatch updated = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(this.stateManager).updateConfiguration();

        ApplicationManager.getApplication().invokeAndWait(afterCompletion);

        Assertions.assertTrue(updated.await(10, TimeUnit.SECONDS));
    }

    private void assertRunsAndRefreshesDescription(Consumer<DdevRunner> action, String expectedCommandLine, String expectedTitle) throws InterruptedException {
        final ArgumentCaptor<GeneralCommandLine> commandLine = ArgumentCaptor.forClass(GeneralCommandLine.class);
        final ArgumentCaptor<Runnable> afterCompletion = ArgumentCaptor.forClass(Runnable.class);
        final CountDownLatch updated = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            updated.countDown();
            return null;
        }).when(this.stateManager).updateDescription();

        action.accept(new DdevRunnerImpl());

        Mockito.verify(this.runner).run(commandLine.capture(), Mockito.eq(expectedTitle), afterCompletion.capture());
        Assertions.assertEquals(expectedCommandLine, commandLine.getValue().getCommandLineString());
        Assertions.assertEquals(Path.of(getProject().getBasePath()), commandLine.getValue().getWorkDirectory().toPath());

        afterCompletion.getValue().run();

        Assertions.assertTrue(updated.await(10, TimeUnit.SECONDS));
    }
}
