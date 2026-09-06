package de.php_perfect.intellij.ddev.docker;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * The Docker provider DDEV talks to. The stored value is the label, so settings stay readable.
 */
public enum DockerProvider {
    AUTO_DETECT("Auto-detect"),
    DOCKER_DESKTOP("Docker Desktop"),
    DOCKER_CE("Docker CE"),
    ORBSTACK("OrbStack"),
    COLIMA("Colima"),
    RANCHER_DESKTOP("Rancher Desktop");

    private final @NotNull String value;

    DockerProvider(@NotNull String value) {
        this.value = value;
    }

    public @NotNull String value() {
        return this.value;
    }

    @Override
    public @NotNull String toString() {
        return this.value;
    }

    public static @NotNull DockerProvider fromValue(@Nullable String value) {
        return Arrays.stream(values()).filter(provider -> provider.value.equals(value)).findFirst().orElse(AUTO_DETECT);
    }

    /**
     * Providers that can run on the current operating system.
     */
    public static @NotNull List<DockerProvider> available() {
        return Arrays.stream(values()).filter(DockerProvider::isSupported).toList();
    }

    public boolean isSupported() {
        return switch (this) {
            case AUTO_DETECT, DOCKER_DESKTOP, RANCHER_DESKTOP -> true;
            case DOCKER_CE -> SystemInfo.isLinux;
            case ORBSTACK -> SystemInfo.isMac;
            case COLIMA -> SystemInfo.isMac || SystemInfo.isLinux;
        };
    }
}
