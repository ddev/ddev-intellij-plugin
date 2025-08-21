package de.php_perfect.intellij.ddev.status_bar;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.impl.status.EditorBasedStatusBarPopup;
import com.intellij.util.concurrency.EdtExecutorService;
import com.intellij.util.messages.MessageBus;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.StateChangedListener;
import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.icons.DdevIntegrationIcons;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.State;
import de.php_perfect.intellij.ddev.tutorial.GotItTutorial;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

import static com.intellij.util.ui.update.UiNotifyConnector.doWhenFirstShown;

public final class DdevStatusBarWidgetImpl extends EditorBasedStatusBarPopup {
    private static final @NotNull Logger LOG = Logger.getInstance(DdevStatusBarWidgetImpl.class);
    private static final Key<State> DDEV_STATE_KEY = new Key<>("DdevIntegration.State");
    private static final @NotNull String ACTION_GROUP = "DdevIntegration.Services";
    public static final @NotNull String WIDGET_ID = "DdevStatusBarWidget";

    public DdevStatusBarWidgetImpl(@NotNull Project project) {
        super(project, false);
    }

    @Override
    public @NonNls @NotNull String ID() {
        return WIDGET_ID;
    }

    @Override
    public void install(@NotNull StatusBar statusBar) {
        if (statusBar.getProject() != null && !statusBar.getProject().equals(this.getProject())) {
            LOG.warn("Cannot install widget from one project on status bar of another project");
            return;
        }

        super.install(statusBar);
        registerUpdateListener();
        doWhenFirstShown(super.getComponent(), () -> delayTutorial(super.getComponent()), this);
    }

    private void registerUpdateListener() {
        MessageBus messageBus = this.getProject().getMessageBus();
        messageBus.connect(this).subscribe(StateChangedListener.DDEV_CHANGED, new StatusBarUpdateListener());
    }

    private final class StatusBarUpdateListener implements StateChangedListener {
        @Override
        public void onDdevChanged(@NotNull State state) {
            LOG.debug("Status bar received state update: available=" + state.isAvailable() +
                     ", configured=" + state.isConfigured() +
                     ", binary=" + state.getDdevBinary() +
                     ", version=" + state.getDdevVersion());
            putState(state);
            DdevStatusBarWidgetImpl.this.update();
        }

        private void putState(State state) {
            DdevStatusBarWidgetImpl.this.getProject().putUserData(DDEV_STATE_KEY, state);
        }
    }

    @Override
    protected @NotNull ListPopup createPopup(@NotNull DataContext context) {
        ActionGroup group = (ActionGroup) ActionManager.getInstance().getAction(ACTION_GROUP);
        String place = ActionPlaces.getPopupPlace(ActionPlaces.STATUS_BAR_PLACE);

        return JBPopupFactory.getInstance().createActionGroupPopup(null, group, context, JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false, place);
    }

    @Override
    protected @NotNull StatusBarWidget createInstance(@NotNull Project project) {
        return new DdevStatusBarWidgetImpl(project);
    }

    @Override
    protected @NotNull WidgetState getWidgetState(@Nullable VirtualFile file) {
        State state = this.fetchState();

        // Enhanced logging to help diagnose status indicator issues
        if (!state.isAvailable()) {
            LOG.debug("Status indicator hidden: DDEV not available (version: " + state.getDdevVersion() + ", binary: " + state.getDdevBinary() + ")");
            return WidgetState.HIDDEN;
        }

        if (!state.isConfigured()) {
            LOG.debug("Status indicator hidden: DDEV not configured (no .ddev/config.yaml found)");
            return WidgetState.HIDDEN;
        }

        LOG.debug("Status indicator visible: DDEV available and configured");
        String toolTipText = DdevIntegrationBundle.message("statusBar.toolTip");
        String statusText = this.getStatusText();
        WidgetState widgetState = new WidgetState(toolTipText, statusText, true);
        widgetState.setIcon(DdevIntegrationIcons.DdevLogoMono);

        return widgetState;
    }

    private void delayTutorial(@NotNull JComponent component) {
        EdtExecutorService.getScheduledExecutorInstance().schedule(() -> {
            if (!this.isDisposed()) {
                GotItTutorial.getInstance().showStatusBarTutorial(component, this);
            }
        }, 3000, TimeUnit.MILLISECONDS);
    }

    private @NotNull @NlsContexts.StatusBarText String getStatusText() {
        State state = this.fetchState();
        Description description = state.getDescription();
        Description.Status status = description != null ? description.getStatus() : null;

        return this.buildStatusMessage(status);
    }

    private @NotNull @NlsContexts.StatusBarText String buildStatusMessage(@Nullable Description.Status status) {
        if (status == null) {
            return DdevIntegrationBundle.message("status.Undefined");
        }

        return switch (status) {
            case RUNNING -> DdevIntegrationBundle.message("status.Running");
            case STARTING -> DdevIntegrationBundle.message("status.Starting");
            case STOPPED -> DdevIntegrationBundle.message("status.Stopped");
            case DIR_MISSING -> DdevIntegrationBundle.message("status.DirMissing");
            case CONFIG_MISSING -> DdevIntegrationBundle.message("status.ConfigMissing");
            case PAUSED -> DdevIntegrationBundle.message("status.Paused");
            case UNHEALTHY -> DdevIntegrationBundle.message("status.Unhealthy");
        };
    }

    private State fetchState() {
        // First try to get cached state from user data
        State state = this.getProject().getUserData(DDEV_STATE_KEY);

        // If no cached state, get current state directly from state manager
        // This handles the case where the widget is checked before state initialization completes
        if (state == null) {
            LOG.debug("No cached state found, fetching current state from DdevStateManager");
            state = DdevStateManager.getInstance(this.getProject()).getState();
        }

        return state;
    }
}
