package de.php_perfect.intellij.ddev.error_reporting;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import io.sentry.Hint;
import io.sentry.Sentry;
import io.sentry.SentryEvent;
import io.sentry.SentryLevel;
import io.sentry.SentryOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

final class SentrySdkInitializerTest extends BasePlatformTestCase {
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
    @SuppressWarnings({"unchecked", "rawtypes"})
    void configuresSdkAndScrubsEvents() {
        final ArgumentCaptor<Sentry.OptionsConfiguration> configuration = ArgumentCaptor.forClass(Sentry.OptionsConfiguration.class);

        try (MockedStatic<Sentry> sentry = Mockito.mockStatic(Sentry.class)) {
            SentrySdkInitializer.init();

            sentry.verify(() -> Sentry.init(configuration.capture()));
        }

        final SentryOptions options = new SentryOptions();
        configuration.getValue().configure(options);

        Assertions.assertTrue(options.getDsn().endsWith("@o1261149.ingest.sentry.io/6438173"));
        Assertions.assertFalse(options.isEnableUncaughtExceptionHandler());

        final SentryEvent event = new SentryEvent();
        event.setServerName("workstation");
        event.setEnvironment("production");
        event.setLevel(SentryLevel.ERROR);

        final SentryEvent sent = options.getBeforeSend().execute(event, new Hint());

        Assertions.assertSame(event, sent);
        Assertions.assertNull(sent.getServerName());
        Assertions.assertNull(sent.getEnvironment());
        Assertions.assertNull(sent.getLevel());
        Assertions.assertEquals(ApplicationInfo.getInstance().getFullVersion(), sent.getContexts().getRuntime().getVersion());
        Assertions.assertEquals(System.getProperty("os.name"), sent.getContexts().getOperatingSystem().getName());
        Assertions.assertEquals(System.getProperty("java.version"), sent.getExtra("jdk.version"));
        Assertions.assertEquals(System.getProperty("java.vendor"), sent.getExtra("jdk.vendor"));
    }
}
