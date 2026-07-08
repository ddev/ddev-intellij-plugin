package de.php_perfect.intellij.ddev.state;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
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

        this.checkChanged(() -> {
            this.resetState();
            this.checkIsInstalled();
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

    private void checkIsInstalled() {
        DdevSettingsState configurable = DdevSettingsState.getInstance(this.project);
        String ddevBinary = configurable.ddevBinary;

        if (!ddevBinary.isEmpty() && isBinaryMissing(ddevBinary)) {
            // The configured override no longer exists, e.g. DDEV was uninstalled or moved into WSL
            // (https://github.com/ddev/ddev-intellij-plugin/issues/250). Fall back to PATH detection
            // instead of failing every subsequent command execution with an error.
            LOG.warn(String.format("Configured ddev binary %s no longer exists, falling back to PATH detection", ddevBinary));
            ddevBinary = "";
        }

        if (ddevBinary.isEmpty()) {
            // The binary setting is only an override; by default ddev is looked up on the PATH.
            ddevBinary = Objects.requireNonNullElse(BinaryLocator.getInstance().findInPath(this.project), "");
        }

        this.state.setDdevBinary(ddevBinary);
    }

    private static boolean isBinaryMissing(@NotNull String binaryPath) {
        final Path path;

        try {
            path = Paths.get(binaryPath);
        } catch (InvalidPathException ignored) {
            return false;
        }

        // Plain command names and non-native paths (e.g. Linux paths while running on Windows) are resolved
        // by the process launcher; only absolute native paths can be verified here.
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
        } catch (CommandFailedException exception) {
            LOG.error(exception);
            this.state.setDdevVersion(null);
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
        } catch (CommandFailedException exception) {
            LOG.error(exception);
            this.state.setDescription(null);
        }
    }
}
