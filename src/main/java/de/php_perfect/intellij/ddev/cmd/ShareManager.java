package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the running {@code ddev share} process of a project so it can be stopped again
 * and notifies about the public share URL as soon as ngrok reports it.
 */
@Service(Service.Level.PROJECT)
public final class ShareManager {
    private static final @NotNull Pattern SHARE_URL_PATTERN = Pattern.compile("https://[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*\\.ngrok(?:-free)?\\.(?:io|app|dev)");
    private static final int MAX_BUFFER_LENGTH = 65_536;

    private final @NotNull Project project;
    private volatile @Nullable ProcessHandler shareProcessHandler;

    public ShareManager(@NotNull Project project) {
        this.project = project;
    }

    public void setShareProcessHandler(@Nullable ProcessHandler processHandler) {
        this.shareProcessHandler = processHandler;

        if (processHandler != null) {
            processHandler.addProcessListener(new ShareUrlNotifyingListener());
        }
    }

    public boolean isSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        return processHandler != null && !processHandler.isProcessTerminated();
    }

    public void stopSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }

        this.shareProcessHandler = null;
    }

    public static @NotNull ShareManager getInstance(@NotNull Project project) {
        return project.getService(ShareManager.class);
    }

    /**
     * Scans the {@code ddev share} output for the public ngrok URL, which is otherwise easy to miss
     * in the run console (https://github.com/ddev/ddev-intellij-plugin/issues/8).
     */
    private final class ShareUrlNotifyingListener implements ProcessListener {
        private final @NotNull StringBuilder buffer = new StringBuilder();
        private boolean notified = false;

        @Override
        public synchronized void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
            if (this.notified || this.buffer.length() > MAX_BUFFER_LENGTH) {
                return;
            }

            this.buffer.append(event.getText());

            final Matcher matcher = SHARE_URL_PATTERN.matcher(this.buffer);

            if (matcher.find()) {
                this.notified = true;
                DdevNotifier.getInstance(ShareManager.this.project).notifyShareUrl(matcher.group());
            }
        }
    }
}
