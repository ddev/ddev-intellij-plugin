package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class StateWatcherImpl implements StateWatcher, Disposable {
    private static final @NotNull Logger LOG = Logger.getInstance(StateWatcherImpl.class);

    private @Nullable ScheduledFuture<?> scheduledFuture = null;

    private final @NotNull Project project;

    public StateWatcherImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void startWatching() {
        if (this.isWatching()) {
            this.stopWatching();
        }

        this.scheduledFuture = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            LOG.debug("DDEV state watcher triggering update");
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                DdevStateManager ddevStateManager = DdevStateManager.getInstance(this.project);
                ddevStateManager.updateConfiguration();
                ddevStateManager.updateDescription();
            });
        }, 10L, 10L, TimeUnit.SECONDS);
        LOG.info("DDEV state watcher started");
    }

    @Override
    public void stopWatching() {
        if (this.scheduledFuture != null) {
            this.scheduledFuture.cancel(true);
        }
        LOG.info("DDEV state watcher stopped");
    }

    @Override
    public boolean isWatching() {
        return this.scheduledFuture != null && !this.scheduledFuture.isCancelled();
    }

    @Override
    public void dispose() {
        this.stopWatching();
    }
}
