package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicBoolean;

public final class UnknownStateListener implements StateChangedListener {
    private final Project project;
    private final AtomicLong lastNullDescriptionTime = new AtomicLong(0);
    private final AtomicBoolean notificationScheduled = new AtomicBoolean(false);
    private static final long NOTIFICATION_DELAY_MS = 30_000;

    public UnknownStateListener(Project project) {
        this.project = project;
    }

    @Override
    public void onDdevChanged(@NotNull State state) {
        if (!isDdevReady(state)) {
            resetTimer();
            return;
        }

        Description description = state.getDescription();

        if (isDescriptionMissing(description)) {
            handleMissingDescription();
        } else {
            resetTimer();
        }
    }

    private boolean isDdevReady(@NotNull State state) {
        return state.isAvailable() && state.isConfigured();
    }

    private boolean isDescriptionMissing(Description description) {
        return description == null || description.getStatus() == null;
    }

    private void handleMissingDescription() {
        long currentTime = System.currentTimeMillis();

        if (lastNullDescriptionTime.compareAndSet(0, currentTime)) {
            scheduleDelayedNotificationCheck();
        }
    }

    private void scheduleDelayedNotificationCheck() {
        if (notificationScheduled.compareAndSet(false, true)) {
            ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    Thread.sleep(NOTIFICATION_DELAY_MS);
                    checkAndNotifyIfStillUnknown();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    notificationScheduled.set(false);
                }
            });
        }
    }

    private void checkAndNotifyIfStillUnknown() {
        State currentState = DdevStateManager.getInstance(this.project).getState();
        Description currentDescription = currentState.getDescription();

        if (isDdevReady(currentState) && isDescriptionMissing(currentDescription)) {
            DdevNotifier.getInstance(this.project).notifyUnknownStateEntered();
        }
    }

    private void resetTimer() {
        lastNullDescriptionTime.set(0);
        notificationScheduled.set(false);
    }
}
