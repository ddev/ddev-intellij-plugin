package de.php_perfect.intellij.ddev.docker;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.concurrency.AppExecutorUtil;
import de.php_perfect.intellij.ddev.DdevIntegrationBundle;
import de.php_perfect.intellij.ddev.cmd.Docker;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import de.php_perfect.intellij.ddev.settings.DdevSettingsState;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.terminal.DdevTerminalService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starts the configured Docker provider and reloads the DDEV state once Docker answers.
 */
public final class DockerProviderStarter {
    private static final Logger LOG = Logger.getInstance(DockerProviderStarter.class);
    private static final int POLL_SECONDS = 3;
    private static final int POLL_ATTEMPTS = 60;
    // GUI-launched IDEs on macOS do not inherit the shell PATH, so the Homebrew prefixes are searched too.
    private static final @NotNull List<String> EXTRA_BIN_DIRECTORIES = List.of("/opt/homebrew/bin", "/usr/local/bin");

    private DockerProviderStarter() {
    }

    public static void start(@NotNull Project project) {
        final DdevSettingsState settings = DdevSettingsState.getInstance(project);
        final DockerProvider configured = DockerProvider.fromValue(settings.dockerProvider);
        final DockerProvider provider = configured == DockerProvider.AUTO_DETECT ? detect() : configured;

        if (provider == null) {
            DdevNotifier.getInstance(project).notifyDockerProviderStartFailed(
                    DdevIntegrationBundle.message("docker.start.notDetected"));
            return;
        }

        if (provider == DockerProvider.DOCKER_CE) {
            final DdevTerminalService terminal = DdevTerminalService.getInstance(project);
            if (terminal == null) {
                DdevNotifier.getInstance(project).notifyDockerProviderStartFailed(
                        DdevIntegrationBundle.message("docker.start.terminalRequired"));
                return;
            }
            terminal.openCommand(List.of("sudo", "systemctl", "start", "docker"),
                    DdevIntegrationBundle.message("docker.start.title", provider.value()), null);
            waitForDocker(project, provider);
            return;
        }

        final List<String> command = command(provider, settings.colimaArguments);
        if (command == null) {
            DdevNotifier.getInstance(project).notifyDockerProviderStartFailed(
                    DdevIntegrationBundle.message("docker.start.notInstalled", provider.value()));
            return;
        }

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            try {
                new GeneralCommandLine(command).createProcess();
                waitForDocker(project, provider);
            } catch (ExecutionException exception) {
                LOG.info("Starting " + provider.value() + " failed", exception);
                DdevNotifier.getInstance(project).notifyDockerProviderStartFailed(exception.getMessage());
            }
        });
    }

    static @Nullable List<String> command(@NotNull DockerProvider provider, @NotNull String colimaArguments) {
        return switch (provider) {
            case DOCKER_DESKTOP -> {
                if (SystemInfo.isMac) yield List.of("open", "-a", "Docker");
                if (SystemInfo.isWindows) yield existing(programFiles("Docker", "Docker", "Docker Desktop.exe"));
                yield List.of("systemctl", "--user", "start", "docker-desktop");
            }
            case ORBSTACK -> binary("orb", "start");
            case COLIMA -> {
                final List<String> colima = binary("colima", "start");
                if (colima == null) yield null;
                final List<String> arguments = new ArrayList<>(colima);
                Arrays.stream(colimaArguments.trim().split("\\s+")).filter(argument -> !argument.isEmpty()).forEach(arguments::add);
                yield arguments;
            }
            case RANCHER_DESKTOP -> {
                if (SystemInfo.isMac) yield List.of("open", "-a", "Rancher Desktop");
                if (SystemInfo.isWindows) yield existing(programFiles("Rancher Desktop", "Rancher Desktop.exe"));
                yield binary("rdctl", "start");
            }
            case DOCKER_CE, AUTO_DETECT -> null;
        };
    }

    /**
     * Picks the provider that is installed, preferring the ones DDEV recommends for the platform.
     */
    static @Nullable DockerProvider detect() {
        if (SystemInfo.isMac) {
            if (Files.isDirectory(Path.of("/Applications/OrbStack.app")) || findBinary("orb") != null) return DockerProvider.ORBSTACK;
            if (Files.isDirectory(Path.of("/Applications/Docker.app"))) return DockerProvider.DOCKER_DESKTOP;
            if (findBinary("colima") != null) return DockerProvider.COLIMA;
            if (Files.isDirectory(Path.of("/Applications/Rancher Desktop.app"))) return DockerProvider.RANCHER_DESKTOP;
            return null;
        }
        if (SystemInfo.isWindows) {
            if (programFiles("Docker", "Docker", "Docker Desktop.exe").toFile().isFile()) return DockerProvider.DOCKER_DESKTOP;
            if (programFiles("Rancher Desktop", "Rancher Desktop.exe").toFile().isFile()) return DockerProvider.RANCHER_DESKTOP;
            return null;
        }
        final String home = System.getProperty("user.home");
        if (Files.exists(Path.of("/usr/lib/systemd/user/docker-desktop.service"))
                || Files.exists(Path.of(home, ".config", "systemd", "user", "docker-desktop.service"))
                || Files.isDirectory(Path.of("/opt/docker-desktop"))) {
            return DockerProvider.DOCKER_DESKTOP;
        }
        if (findBinary("colima") != null) return DockerProvider.COLIMA;
        if (findBinary("rdctl") != null) return DockerProvider.RANCHER_DESKTOP;
        return findBinary("dockerd") != null || Files.exists(Path.of("/usr/lib/systemd/system/docker.service"))
                ? DockerProvider.DOCKER_CE : null;
    }

    private static void waitForDocker(@NotNull Project project, @NotNull DockerProvider provider) {
        final AtomicInteger attempts = new AtomicInteger();
        final AtomicReference<ScheduledFuture<?>> poll = new AtomicReference<>();
        poll.set(AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(() -> {
            if (project.isDisposed()) {
                poll.get().cancel(false);
                return;
            }
            if (Docker.getInstance().isRunning(project.getBasePath())) {
                poll.get().cancel(false);
                DdevStateManager.getInstance(project).reinitialize();
            } else if (attempts.incrementAndGet() >= POLL_ATTEMPTS) {
                poll.get().cancel(false);
                DdevNotifier.getInstance(project).notifyDockerProviderStartFailed(
                        DdevIntegrationBundle.message("docker.start.timeout", provider.value()));
            }
        }, POLL_SECONDS, POLL_SECONDS, TimeUnit.SECONDS));
    }

    private static @Nullable List<String> binary(@NotNull String name, @NotNull String... arguments) {
        final String binary = findBinary(name);
        if (binary == null) {
            return null;
        }
        final List<String> command = new ArrayList<>();
        command.add(binary);
        command.addAll(List.of(arguments));
        return command;
    }

    static @Nullable String findBinary(@NotNull String name) {
        final List<String> directories = new ArrayList<>();
        final String path = System.getenv("PATH");
        if (path != null) {
            directories.addAll(Arrays.asList(path.split(File.pathSeparator)));
        }
        directories.addAll(EXTRA_BIN_DIRECTORIES);

        final List<String> fileNames = SystemInfo.isWindows ? List.of(name + ".exe", name) : List.of(name);
        for (String directory : directories) {
            if (directory.isBlank()) {
                continue;
            }
            for (String fileName : fileNames) {
                try {
                    final Path candidate = Path.of(directory, fileName);
                    if (Files.isRegularFile(candidate) && Files.isExecutable(candidate)) {
                        return candidate.toString();
                    }
                } catch (InvalidPathException ignored) {
                    // PATH may contain entries that are not valid paths on this system.
                }
            }
        }
        return null;
    }

    private static @NotNull Path programFiles(@NotNull String first, @NotNull String... more) {
        final String programFiles = System.getenv().getOrDefault("ProgramFiles", "C:\\Program Files");
        return Path.of(programFiles, first).resolve(Path.of("", more));
    }

    private static @Nullable List<String> existing(@NotNull Path executable) {
        return Files.isRegularFile(executable) ? List.of(executable.toString()) : null;
    }
}
