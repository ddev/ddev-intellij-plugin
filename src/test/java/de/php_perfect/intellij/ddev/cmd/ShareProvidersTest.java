package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class ShareProvidersTest {
    @TempDir
    private Path projectRoot;

    @Test
    void listsBuiltInProvidersFirstAndThenCustomScripts() throws Exception {
        final Path providers = Files.createDirectories(this.projectRoot.resolve(".ddev/share-providers"));
        Files.writeString(providers.resolve("ngrok.sh"), "");
        Files.writeString(providers.resolve("zrok.sh"), "");
        Files.writeString(providers.resolve("expose.sh"), "");
        Files.writeString(providers.resolve("README.txt"), "");

        assertThat(ShareProviders.list(this.projectRoot.toString()))
                .containsExactly("ngrok", "cloudflared", "expose", "zrok");
    }

    @Test
    void offersTheBuiltInProvidersWithoutAProjectDirectory() {
        assertThat(ShareProviders.list(null)).containsExactly("ngrok", "cloudflared");
        assertThat(ShareProviders.list(this.projectRoot.toString())).containsExactly("ngrok", "cloudflared");
    }

    @Test
    void knowsHowToInstallOnlyTheBuiltInTunnelBinaries() {
        assertThat(ShareProviders.installUrl("cloudflared")).startsWith("https://developers.cloudflare.com/");
        assertThat(ShareProviders.homebrewFormula("ngrok")).isEqualTo("ngrok");
        assertThat(ShareProviders.installUrl("zrok")).isNull();
        assertThat(ShareProviders.homebrewFormula("zrok")).isNull();
    }
}
