package de.php_perfect.intellij.ddev.error_reporting;

import com.intellij.openapi.application.ApplicationInfo;
import io.sentry.Sentry;
import io.sentry.protocol.OperatingSystem;
import io.sentry.protocol.SentryRuntime;

public class SentrySdkInitializer {
    private static final String DSN = "https://92fd27b7c1fe48a98b040b3a1b603570@o1261149.ingest.sentry.io/6438173";

    private SentrySdkInitializer() {
    }

    public static void init() {
        Sentry.init(options -> {
            options.setDsn(DSN);
            options.setEnableUncaughtExceptionHandler(false);

            options.setBeforeSend((event, hint) -> {
                event.setServerName(null);
                event.setEnvironment(null);
                event.setLevel(null);

                event.getContexts().setRuntime(buildRuntimeContext());
                event.getContexts().setOperatingSystem(buildOperatingSystemContext());

                event.setExtra("jdk.vendor", System.getProperty("java.vendor"));
                event.setExtra("jdk.version", System.getProperty("java.version"));

                return event;
            });
        });
    }

    private static SentryRuntime buildRuntimeContext() {
        SentryRuntime runtime = new SentryRuntime();

        runtime.setName(ApplicationInfo.getInstance().getVersionName());
        runtime.setVersion(ApplicationInfo.getInstance().getFullVersion());
        runtime.setRawDescription(ApplicationInfo.getInstance().getBuild().asString());

        return runtime;
    }

    private static OperatingSystem buildOperatingSystemContext() {
        OperatingSystem os = new OperatingSystem();

        os.setName(System.getProperty("os.name"));
        os.setVersion(System.getProperty("os.version"));

        return os;
    }
}
