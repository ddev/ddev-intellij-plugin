package de.php_perfect.intellij.ddev.state;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectCloseListener;
import com.intellij.openapi.util.Key;
import de.php_perfect.intellij.ddev.StateInitializedListener;
import de.php_perfect.intellij.ddev.cmd.DdevRunner;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.cmd.Docker;
import de.php_perfect.intellij.ddev.cmd.wsl.WslAware;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.util.DdevProjectRoot;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the DDEV project when its IDE window opens and stops it when the last window using it
 * closes, if the user enabled that.
 */
public final class AutoStartStop {
    private static final Logger LOG = Logger.getInstance(AutoStartStop.class);
    private static final Key<Boolean> START_CHECKED = Key.create("de.php_perfect.intellij.ddev.autoStartChecked");

    private AutoStartStop() {
    }

    public static final class StartListener implements StateInitializedListener {
        private final @NotNull Project project;

        public StartListener(@NotNull Project project) {
            this.project = project;
        }

        @Override
        public void onStateInitialized(@NotNull State state) {
            final String projectRoot = DdevProjectRoot.of(this.project);

            if (!DdevSettingsState.getInstance(this.project).automaticallyStartProject
                    || !state.isConfigured() || projectRoot == null) {
                return;
            }

            ProjectLocks.shared().acquire(projectRoot, this.project);

            // The state is initialized again on every reload; only the first one may start the project.
            if (this.project.getUserData(START_CHECKED) != null) {
                return;
            }
            this.project.putUserData(START_CHECKED, Boolean.TRUE);

            final Description description = state.getDescription();
            if ((description != null && description.getStatus() == Description.Status.RUNNING)
                    || !Docker.getInstance().isRunning(projectRoot)) {
                return;
            }

            DdevRunner.getInstance().start(this.project);
        }
    }

    public static final class StopListener implements ProjectCloseListener {
        @Override
        public void projectClosing(@NotNull Project project) {
            final String projectRoot = DdevProjectRoot.of(project);

            if (!DdevSettingsState.getInstance(project).automaticallyStartProject || projectRoot == null) {
                return;
            }

            final boolean inUse = ProjectLocks.shared().releaseAndCheckInUse(projectRoot, project);
            final State state = DdevStateManager.getInstance(project).getState();
            final Description description = state.getDescription();

            if (inUse || state.getDdevBinary() == null || description == null || description.getName() == null
                    || description.getStatus() != Description.Status.RUNNING) {
                return;
            }

            final GeneralCommandLine commandLine = new GeneralCommandLine(state.getDdevBinary(), "stop", description.getName())
                    .withWorkDirectory(projectRoot)
                    .withEnvironment("DDEV_NONINTERACTIVE", "true");
            if (DdevSettingsState.getInstance(project).createSnapshotOnStop) {
                commandLine.addParameter("--snapshot");
            }

            // The stop outlives the IDE window, so it runs without a console or process handler.
            final AtomicReference<GeneralCommandLine> patched = new AtomicReference<>();
            ProgressManager.getInstance().runProcess(
                    () -> patched.set(WslAware.patchCommandLine(commandLine)), new EmptyProgressIndicator());
            try {
                patched.get().createProcess();
            } catch (ExecutionException exception) {
                LOG.info("Could not stop the DDEV project " + description.getName(), exception);
            }
        }
    }
}
