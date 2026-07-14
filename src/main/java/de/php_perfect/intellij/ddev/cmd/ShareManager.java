package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.wordpress.WordPressShareSupport;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the running {@code ddev share} process of a project so it can be stopped again
 * and notifies about the public share URL as soon as ngrok reports it.
 */
@Service(Service.Level.PROJECT)
public final class ShareManager {
    private static final @NotNull Pattern TUNNEL_URL_PATTERN = Pattern.compile(
            "Tunnel URL:\\s*(https://[^\\s]+)");
    private static final @NotNull Pattern FORWARDING_URL_PATTERN = Pattern.compile(
            "Forwarding\\s+(https://[^\\s]+)\\s+->");
    private static final @NotNull Pattern PROVIDER_URL_PATTERN = Pattern.compile(
            "https://[A-Za-z0-9-]+(?:\\.[A-Za-z0-9-]+)*(?:\\.ngrok(?:-free)?\\.(?:io|app|dev)|\\.trycloudflare\\.com)");
    private static final @NotNull Pattern SHARE_ERROR_PATTERN = Pattern.compile("ERR_NGROK_\\d+");
    private static final int MAX_BUFFER_LENGTH = 65_536;

    private final @NotNull Project project;
    private volatile @Nullable ProcessHandler shareProcessHandler;
    private volatile @Nullable String sharedWorkingDirectory;
    private @Nullable WordPressShareSupport.Session wordpressSession;

    public ShareManager(@NotNull Project project) {
        this.project = project;
    }

    public void setShareProcessHandler(@Nullable ProcessHandler processHandler) {
        this.setShareProcessHandler(processHandler, this.project.getBasePath());
    }

    public synchronized void setShareProcessHandler(@Nullable ProcessHandler processHandler,
                                                    @Nullable String workingDirectory) {
        this.cleanupWordPressShare();
        this.shareProcessHandler = processHandler;
        this.sharedWorkingDirectory = workingDirectory;

        if (processHandler != null) {
            processHandler.addProcessListener(new ShareUrlNotifyingListener());
        }
    }

    public boolean isSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        return processHandler != null && !processHandler.isProcessTerminated();
    }

    public boolean isSharing(@Nullable String workingDirectory) {
        return this.isSharing() && Objects.equals(this.sharedWorkingDirectory, workingDirectory);
    }

    public synchronized void stopSharing() {
        final ProcessHandler processHandler = this.shareProcessHandler;

        if (processHandler != null && !processHandler.isProcessTerminated()) {
            processHandler.destroyProcess();
        }

        this.shareProcessHandler = null;
        this.sharedWorkingDirectory = null;
        this.cleanupWordPressShare();
    }

    private synchronized void configureWordPressShare(@NotNull String url) {
        if (this.wordpressSession != null || this.sharedWorkingDirectory == null || !this.isSharing()) {
            return;
        }
        try {
            this.wordpressSession = WordPressShareSupport.start(Path.of(this.sharedWorkingDirectory), url);
        } catch (IOException exception) {
            DdevNotifier.getInstance(this.project).notifyWordPressShareSetupFailed(exception.getMessage());
        }
    }

    private synchronized void cleanupWordPressShare() {
        if (this.wordpressSession == null) {
            return;
        }
        try {
            this.wordpressSession.close();
        } catch (IOException exception) {
            DdevNotifier.getInstance(this.project).notifyWordPressShareSetupFailed(exception.getMessage());
        } finally {
            this.wordpressSession = null;
        }
    }

    public static @NotNull ShareManager getInstance(@NotNull Project project) {
        return project.getService(ShareManager.class);
    }

    static @Nullable String extractShareUrl(@NotNull CharSequence output) {
        for (Pattern pattern : List.of(TUNNEL_URL_PATTERN, FORWARDING_URL_PATTERN, PROVIDER_URL_PATTERN)) {
            final Matcher matcher = pattern.matcher(output);
            if (matcher.find()) {
                return matcher.groupCount() > 0 ? matcher.group(1) : matcher.group();
            }
        }
        return null;
    }

    /**
     * Scans the {@code ddev share} output for the public ngrok URL, which is otherwise easy to miss
     * in the run console, and for ngrok errors such as a missing authtoken
     * (https://github.com/ddev/ddev-intellij-plugin/issues/8).
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

            final String shareUrl = extractShareUrl(this.buffer);
            if (shareUrl != null) {
                this.notified = true;
                ShareManager.this.configureWordPressShare(shareUrl);
                DdevNotifier.getInstance(ShareManager.this.project).notifyShareUrl(shareUrl);
                return;
            }

            final Matcher errorMatcher = SHARE_ERROR_PATTERN.matcher(this.buffer);

            if (errorMatcher.find()) {
                this.notified = true;
                DdevNotifier.getInstance(ShareManager.this.project).notifyShareFailed(errorMatcher.group());
            }
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
            ShareManager.this.cleanupWordPressShare();
        }
    }
}
