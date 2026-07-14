package de.php_perfect.intellij.ddev.cmd;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/** Loads and caches the same upstream DDEV schema used by the VS Code extension. */
@Service(Service.Level.APP)
public final class DdevConfigOptionsLoader {
    private static final @NotNull String SCHEMA_URL =
            "https://raw.githubusercontent.com/ddev/ddev/main/pkg/ddevapp/schema.json";
    private static final @NotNull Logger LOG = Logger.getInstance(DdevConfigOptionsLoader.class);

    private volatile DdevConfigOptions cached;

    public @NotNull DdevConfigOptions load(@NotNull ProgressIndicator indicator) {
        final DdevConfigOptions existing = this.cached;

        if (existing != null) {
            return existing;
        }

        indicator.checkCanceled();

        try {
            final String json = HttpRequests.request(SCHEMA_URL)
                    .accept("application/json")
                    .readString(indicator);
            this.cached = DdevConfigOptions.parse(json);
        } catch (ProcessCanceledException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            LOG.warn("Unable to load the current DDEV configuration schema; using bundled choices", exception);
            this.cached = DdevConfigOptions.fallback();
        }

        return this.cached;
    }

    public static @NotNull DdevConfigOptionsLoader getInstance() {
        return ApplicationManager.getApplication().getService(DdevConfigOptionsLoader.class);
    }
}
