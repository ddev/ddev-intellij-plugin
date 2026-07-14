package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
