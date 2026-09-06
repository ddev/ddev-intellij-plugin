package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The tunnel providers {@code ddev share --provider} accepts: the built-in ones plus the custom
 * scripts DDEV looks up in {@code .ddev/share-providers/<name>.sh}.
 */
public final class ShareProviders {
    public static final @NotNull List<String> BUILT_IN = List.of("ngrok", "cloudflared");

    private static final @NotNull Map<String, String> INSTALL_URLS = Map.of(
            "ngrok", "https://ngrok.com/download",
            "cloudflared", "https://developers.cloudflare.com/cloudflare-one/networks/connectors/cloudflare-tunnel/downloads/"
    );

    private ShareProviders() {
    }

    public static @NotNull List<String> list(@Nullable String projectRoot) {
        final List<String> providers = new ArrayList<>(BUILT_IN);

        if (projectRoot == null) {
            return providers;
        }

        final Path directory = Path.of(projectRoot, ".ddev", "share-providers");

        if (!Files.isDirectory(directory)) {
            return providers;
        }

        final List<String> custom = new ArrayList<>();
        try (DirectoryStream<Path> scripts = Files.newDirectoryStream(directory, "*.sh")) {
            for (Path script : scripts) {
                final String fileName = script.getFileName().toString();
                final String name = fileName.substring(0, fileName.length() - ".sh".length());
                if (Files.isRegularFile(script) && !providers.contains(name)) {
                    custom.add(name);
                }
            }
        } catch (IOException ignored) {
            // The built-in providers remain usable without the directory listing.
        }
        custom.sort(String::compareTo);
        providers.addAll(custom);
        return providers;
    }

    /**
     * Returns the download page of a provider's tunnel binary, or {@code null} for custom providers.
     */
    public static @Nullable String installUrl(@NotNull String tool) {
        return INSTALL_URLS.get(tool);
    }

    /**
     * Returns the Homebrew formula that installs a provider's tunnel binary, or {@code null} if there is none.
     */
    public static @Nullable String homebrewFormula(@NotNull String tool) {
        return BUILT_IN.contains(tool) ? tool : null;
    }
}
