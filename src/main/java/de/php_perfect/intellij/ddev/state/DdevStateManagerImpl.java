package de.php_perfect.intellij.ddev.state;

import com.intellij.execution.process.ProcessNotCreatedException;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.messages.MessageBus;
import de.php_perfect.intellij.ddev.DatabaseInfoChangedListener;
import de.php_perfect.intellij.ddev.DescriptionChangedListener;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.StateInitializedListener;
import de.php_perfect.intellij.ddev.cmd.*;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DdevStateManagerImpl implements DdevStateManager {
    private static final @NotNull Logger LOG = Logger.getInstance(DdevStateManagerImpl.class);
    private final @NotNull StateImpl state = new StateImpl();
    private final @NotNull Project project;

    // Concurrency control to prevent multiple status checks from running simultaneously
    private final AtomicBoolean isDescriptionUpdateRunning = new AtomicBoolean(false);
    private final AtomicBoolean isConfigurationUpdateRunning = new AtomicBoolean(false);

    // A failing command is reported once until it succeeds again, since the state watcher retries it every few seconds
    private final AtomicBoolean isVersionFailureReported = new AtomicBoolean(false);
    private final AtomicBoolean isDescriptionFailureReported = new AtomicBoolean(false);

    public DdevStateManagerImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public @NotNull State getState() {
        return state;
    }

    @Override
    public void initialize() {
        this.initialize(false);
    }

    @Override
    public void reinitialize() {
        this.initialize(true);
    }

    public void initialize(boolean reinitialize) {
        if (!reinitialize && !Docker.getInstance().isRunning(this.project.getBasePath())) {
            LOG.debug("Docker not available. Skipping initialization");
            DdevNotifier.getInstance(this.project).notifyDockerNotAvailable(Docker.getInstance().getContext(this.project.getBasePath()));

            return;
        }

        if (reinitialize) {
            this.isVersionFailureReported.set(false);
            this.isDescriptionFailureReported.set(false);
        }

        this.checkChanged(() -> {
            this.resetState();
            this.checkIsInstalled(!reinitialize);
            this.checkVersion();
            this.checkConfiguration();
            this.checkDescription();
        });

        LOG.debug("DDEV state initialised " + this.state);
        MessageBus messageBus = this.project.getMessageBus();
        messageBus.syncPublisher(StateInitializedListener.STATE_INITIALIZED).onStateInitialized(this.state);
    }

    @Override
    public void updateConfiguration() {
        // Prevent concurrent configuration updates
        if (!isConfigurationUpdateRunning.compareAndSet(false, true)) {
            LOG.debug("DDEV configuration update already in progress, skipping");
            return;
        }

        try {
            LOG.debug("Updating DDEV configuration data");
            this.checkChanged(() -> {
                this.checkConfiguration();
                // Only check description if no description update is currently running
                if (!isDescriptionUpdateRunning.get()) {
                    this.checkDescription();
                }
            });
        } finally {
            isConfigurationUpdateRunning.set(false);
        }
    }

    @Override
    public void updateDescription() {
        // Prevent concurrent description updates to avoid multiple 'ddev describe' commands
        if (!isDescriptionUpdateRunning.compareAndSet(false, true)) {
            LOG.debug("DDEV description update already in progress, skipping");
            return;
        }

        try {
            LOG.debug("Updating DDEV description data");
            this.checkChanged(this::checkDescription);
        } finally {
            isDescriptionUpdateRunning.set(false);
        }
    }

    @Override
    public void resetState() {
        this.state.reset();
    }

    private void checkChanged(Runnable runnable) {
        final int oldState = this.state.hashCode();
        final int oldDescription = Objects.hashCode(this.state.getDescription());
        int oldDatabaseInfoHash = 0;
        if (this.state.getDescription() != null) {
            oldDatabaseInfoHash = Objects.hashCode(this.state.getDescription().getDatabaseInfo());
        }

        runnable.run();

        if (this.project.isDisposed()) {
            return;
        }

        if (oldState != this.state.hashCode()) {
            LOG.debug("DDEV state changed: " + this.state);
            MessageBus messageBus = this.project.getMessageBus();
            messageBus.syncPublisher(StateChangedListener.DDEV_CHANGED).onDdevChanged(this.state);

            var newDescription = this.state.getDescription();

            if (oldDescription != Objects.hashCode(newDescription)) {
                messageBus.syncPublisher(DescriptionChangedListener.DESCRIPTION_CHANGED).onDescriptionChanged(this.state.getDescription());

                DatabaseInfo newDatabaseInfo = null;
                if (newDescription != null) {
                    newDatabaseInfo = newDescription.getDatabaseInfo();
                }

                if (oldDatabaseInfoHash != Objects.hashCode(newDatabaseInfo)) {
                    messageBus.syncPublisher(DatabaseInfoChangedListener.DATABASE_INFO_CHANGED_TOPIC).onDatabaseInfoChanged(newDatabaseInfo);
                }
            }
        }
    }

    private void checkIsInstalled(boolean autodetect) {
        DdevSettingsState configurable = DdevSettingsState.getInstance(this.project);

        if (!configurable.ddevBinary.isEmpty() && isBinaryMissing(configurable.ddevBinary)) {
            LOG.warn(String.format("Configured ddev binary %s no longer exists, looking it up on the PATH", configurable.ddevBinary));
            this.detectBinary();
        } else if (autodetect && configurable.ddevBinary.isEmpty()) {
            this.detectBinary();
        }

        this.state.setDdevBinary(configurable.ddevBinary);
    }

    /**
     * Replaces the configured binary with the one found on the PATH.
     *
     * @return whether a binary different from the configured one was found
     */
    private boolean detectBinary() {
        final DdevSettingsState configurable = DdevSettingsState.getInstance(this.project);
        final String detectedDdevBinary = BinaryLocator.getInstance().findInPath(this.project);

        if (detectedDdevBinary == null || detectedDdevBinary.isEmpty() || detectedDdevBinary.equals(configurable.ddevBinary)) {
            return false;
        }

        configurable.ddevBinary = detectedDdevBinary;
        this.state.setDdevBinary(detectedDdevBinary);
        DdevNotifier.getInstance(this.project).notifyDdevDetected(detectedDdevBinary);

        return true;
    }

    private static boolean isBinaryMissing(@NotNull String binaryPath) {
        final Path path;

        try {
            path = Paths.get(binaryPath);
        } catch (InvalidPathException ignored) {
            return false;
        }

        // Plain command names and paths that are not native to the host (e.g. Linux paths on Windows, which
        // only resolve inside WSL) cannot be verified here; those surface as a failure to start the process.
        return path.isAbsolute() && !Files.exists(path);
    }

    private void checkVersion() {
        if (!this.state.isBinaryConfigured()) {
            this.state.setDdevVersion(null);
            this.state.setDescription(null);
            return;
        }

        try {
            this.state.setDdevVersion(Ddev.getInstance().version(Objects.requireNonNull(this.state.getDdevBinary()), this.project));
            this.isVersionFailureReported.set(false);
        } catch (CommandFailedException exception) {
            if (isProcessNotCreated(exception) && this.detectBinary()) {
                this.checkVersion();
                return;
            }

            this.state.setDdevVersion(null);
            this.reportFailure("--version", exception, this.isVersionFailureReported);
        }
    }

    private void checkConfiguration() {
        this.state.setConfigured(DdevConfigLoader.getInstance(this.project).exists());
    }

    private void checkDescription() {
        if (!this.state.isAvailable() || !this.state.isConfigured()) {
            this.state.setDescription(null);
            return;
        }

        try {
            this.state.setDescription(Ddev.getInstance().describe(Objects.requireNonNull(this.state.getDdevBinary()), this.project));
            this.isDescriptionFailureReported.set(false);
        } catch (CommandFailedException exception) {
            this.state.setDescription(null);
            this.reportFailure("describe", exception, this.isDescriptionFailureReported);
        }
    }

    /**
     * Command failures are caused by the local DDEV or Docker setup, so they are shown to the user as a
     * notification instead of being logged as IDE errors.
     */
    private void reportFailure(@NotNull String command, @NotNull CommandFailedException exception, @NotNull AtomicBoolean reported) {
        if (!reported.compareAndSet(false, true)) {
            LOG.debug(exception);
            return;
        }

        LOG.warn(exception);
        final String reason = Objects.requireNonNullElse(exception.getDdevMessage(), exception.getMessage());
        DdevNotifier.getInstance(this.project).notifyDdevCommandFailed(command, reason);
    }

    private static boolean isProcessNotCreated(@NotNull Throwable exception) {
        return ExceptionUtil.findCause(exception, ProcessNotCreatedException.class) != null;
    }
}
