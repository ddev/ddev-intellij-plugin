package de.php_perfect.intellij.ddev.addon;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Startup activity to initialize the AddonCache when the project is opened.
 */
public class AddonCacheStartupActivity implements ProjectActivity {
    private static final Logger LOG = Logger.getInstance(AddonCacheStartupActivity.class);

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        LOG.debug("Initializing AddonCache for project: " + project.getName());

        // Only initialize the cache if DDEV is available and configured
        State state = DdevStateManager.getInstance(project).getState();
        if (state.isAvailable() && state.isConfigured()) {
            // This will create the cache instance and trigger an async refresh in the background
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                AddonCache.getInstance(project);
                LOG.info("AddonCache initialized for project: " + project.getName());
            });
        } else {
            LOG.debug("Skipping AddonCache initialization for project: " + project.getName() + " because DDEV is not available or not configured");
        }

        return Unit.INSTANCE;
    }
}
