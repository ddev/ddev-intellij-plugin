package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

final class ShareManagerTest {
    @Test
    void extractsCurrentDdevTunnelOutput() {
        assertThat(ShareManager.extractShareUrl("Starting cloudflared...\nTunnel URL: https://demo.example.net\n"))
                .isEqualTo("https://demo.example.net");
    }

    @Test
    void extractsNgrokAndCloudflareProviderOutput() {
        assertThat(ShareManager.extractShareUrl("Forwarding https://abc.ngrok-free.app -> http://web:80"))
                .isEqualTo("https://abc.ngrok-free.app");
        assertThat(ShareManager.extractShareUrl("connected at https://random-name.trycloudflare.com"))
                .isEqualTo("https://random-name.trycloudflare.com");
    }

    @Test
    void detectsAMissingTunnelBinary() {
        assertThat(ShareManager.extractMissingTool("Error: cloudflared not found in PATH.\n")).isEqualTo("cloudflared");
        assertThat(ShareManager.extractMissingTool(
                "Error: ngrok not found in PATH. Install from https://ngrok.com/download")).isEqualTo("ngrok");
        assertThat(ShareManager.extractMissingTool("Tunnel URL: https://demo.example.net")).isNull();
    }

    @Test
    void aFinishedShareNeverStopsTheShareThatReplacedIt() {
        final ShareManager shareManager = new ShareManager(mock(Project.class));
        final ProcessHandler first = mock(ProcessHandler.class);
        final ProcessHandler second = mock(ProcessHandler.class);

        shareManager.setShareProcessHandler(first, "/project", null);
        shareManager.setShareProcessHandler(second, "/project", null);
        verify(first).destroyProcess();

        when(first.isProcessTerminated()).thenReturn(true);
        shareManager.stopSharing(first);
        verify(second, never()).destroyProcess();
        assertThat(shareManager.isSharing("/project")).isTrue();

        shareManager.stopSharing(second);
        verify(second).destroyProcess();
        assertThat(shareManager.isSharing()).isFalse();
    }
}
