package de.php_perfect.intellij.ddev.terminal;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.jediterm.core.util.TermSize;
import com.jediterm.terminal.TtyConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.ProxyTtyConnector;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Ends the session with a hangup, like closing a terminal emulator, so {@code ddev ssh} and the
 * shell in the container can exit cleanly; the process is destroyed if it is still alive a second
 * later. The terminal unwraps {@link ProxyTtyConnector}s to reach the process connector.
 */
final class HangupOnCloseTtyConnector implements ProxyTtyConnector {
    private static final Logger LOG = Logger.getInstance(HangupOnCloseTtyConnector.class);

    private final @NotNull TtyConnector connector;
    private final @NotNull Process process;

    HangupOnCloseTtyConnector(@NotNull TtyConnector connector, @NotNull Process process) {
        this.connector = connector;
        this.process = process;
    }

    @Override
    public @NotNull TtyConnector getConnector() {
        return this.connector;
    }

    @Override
    public void close() {
        if (!hangup(this.process)) {
            this.connector.close();
            return;
        }
        AppExecutorUtil.getAppScheduledExecutorService().schedule(() -> {
            if (this.process.isAlive()) {
                LOG.info("Terminal hasn't been terminated by SIGHUP, performing default termination");
                this.connector.close();
            }
        }, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * pty4j's Unix processes offer {@code hangup()}; it is looked up at runtime because the IDE
     * only loads pty4j for its own process handling.
     */
    static boolean hangup(@NotNull Process process) {
        try {
            process.getClass().getMethod("hangup").invoke(process);
            return true;
        } catch (ReflectiveOperationException | RuntimeException exception) {
            return false;
        }
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        return this.connector.read(buf, offset, length);
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        this.connector.write(bytes);
    }

    @Override
    public void write(String string) throws IOException {
        this.connector.write(string);
    }

    @Override
    public boolean isConnected() {
        return this.connector.isConnected();
    }

    @Override
    public void resize(@NotNull TermSize termSize) {
        this.connector.resize(termSize);
    }

    @Override
    public int waitFor() throws InterruptedException {
        return this.connector.waitFor();
    }

    @Override
    public boolean ready() throws IOException {
        return this.connector.ready();
    }

    @Override
    public String getName() {
        return this.connector.getName();
    }
}
