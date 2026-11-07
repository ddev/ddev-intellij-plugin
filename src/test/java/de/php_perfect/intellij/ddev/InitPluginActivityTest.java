package de.php_perfect.intellij.ddev;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.actions.DdevStateStub;
import de.php_perfect.intellij.ddev.error_reporting.SentrySdkInitializer;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import kotlin.coroutines.Continuation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class InitPluginActivityTest extends BasePlatformTestCase {
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
    @SuppressWarnings("unchecked")
    void activityInitializesErrorReportingAndState() throws InterruptedException {
        final DdevStateManager stateManager = DdevStateStub.install(getProject(), getTestRootDisposable(), Mockito.mock(DdevStateManager.class));
        final CountDownLatch initialized = new CountDownLatch(1);
        Mockito.doAnswer(invocation -> {
            initialized.countDown();
            return null;
        }).when(stateManager).initialize();

        try (MockedStatic<SentrySdkInitializer> sentry = Mockito.mockStatic(SentrySdkInitializer.class)) {
            Assertions.assertNull(new InitPluginActivity().execute(getProject(), Mockito.mock(Continuation.class)));

            sentry.verify(SentrySdkInitializer::init);
        }

        Assertions.assertTrue(initialized.await(5, TimeUnit.SECONDS));
    }
}
