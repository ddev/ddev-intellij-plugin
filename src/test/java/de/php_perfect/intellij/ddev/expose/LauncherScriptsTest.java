package de.php_perfect.intellij.ddev.expose;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the generated bash launcher against stand-ins for ddev and docker. The fake ddev records
 * its arguments and plays the container: it resolves /var/www/html against the project root.
 */
@DisabledOnOs(OS.WINDOWS)
final class LauncherScriptsTest {
    @TempDir
    private Path temp;
    private Path projectRoot;
    private Path bin;
    private Path log;

    @BeforeEach
    void setUp() throws Exception {
        this.projectRoot = Files.createDirectories(this.temp.resolve("it's & site"));
        Files.createDirectories(this.projectRoot.resolve(".ddev"));
        this.bin = Files.createDirectories(this.temp.resolve("bin"));
        this.log = this.temp.resolve("ddev.log");
        this.dockerRunning("ddev-site-web\nddev-router");
        executable(this.bin.resolve("ddev"), """
                #!/usr/bin/env bash
                printf '%s|' "$PWD" "$@" > "$DDEV_LOG"
                for arg in "$@"; do
                  case "$arg" in
                    /var/www/html/.ddev/.exposed-*) printf 'edited' >> "$PROJECT_ROOT${arg#/var/www/html}" ;;
                  esac
                done
                printf '%s\\n' "/var/www/html/src/Foo.php:12 error" '{"file":"\\/var\\/www\\/html\\/a.php"}'
                exit "${FAKE_EXIT:-0}"
                """);
    }

    @Test
    void runsInTheContainerDirectoryMatchingTheCurrentOne() throws Exception {
        final Path subfolder = Files.createDirectories(this.projectRoot.resolve("web/app"));

        this.run(subfolder, 0, "plugin", "list --status=active");

        assertThat(this.recorded()).containsExactly(this.projectRoot.toString(), "exec", "--quiet", "--raw",
                "--dir", "/var/www/html/web/app", "--", "wp", "plugin", "list --status=active");
    }

    @Test
    void translatesProjectPathsInArguments() throws Exception {
        this.run(this.temp, 0, this.projectRoot.resolve("src/Foo.php").toString(),
                "--config=" + this.projectRoot.resolve("phpstan.neon"));

        assertThat(this.recorded()).containsSubsequence("--dir", "/var/www/html", "--", "wp",
                "/var/www/html/src/Foo.php", "--config=/var/www/html/phpstan.neon");
    }

    @Test
    void copiesFilesOutsideTheProjectInAndBackOut() throws Exception {
        final Path outside = this.temp.resolve("report.txt");
        Files.writeString(outside, "original ");

        this.run(this.temp, 0, outside.toString());

        final String copied = this.recorded().get(this.recorded().size() - 1);
        assertThat(copied).startsWith("/var/www/html/.ddev/.exposed-").endsWith("-report.txt");
        assertThat(Files.readString(outside)).isEqualTo("original edited");
        try (var files = Files.list(this.projectRoot.resolve(".ddev"))) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void translatesContainerPathsInPipedOutputAndKeepsTheExitCode() throws Exception {
        final String output = this.run(this.temp, 3, "cli", "info");

        final String json = this.projectRoot.toString().replace("/", "\\/");
        assertThat(output).isEqualTo(this.projectRoot + "/src/Foo.php:12 error\n{\"file\":\"" + json + "\\/a.php\"}\n");
    }

    @Test
    void refusesToStartAStoppedProject() throws Exception {
        this.dockerRunning("ddev-other-web");

        final Process process = this.start(this.temp, 0);
        final String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);

        assertThat(process.waitFor()).isEqualTo(1);
        assertThat(error).contains("The DDEV project site is not running");
        assertThat(this.log).doesNotExist();
    }

    @Test
    void omitsTheQuietFlagForDdevVersionsWithoutIt() throws Exception {
        this.writeLauncher(false);
        this.run(this.temp, 0);

        assertThat(this.recorded()).containsExactly(this.projectRoot.toString(), "exec", "--raw", "--dir",
                "/var/www/html", "--", "wp");
    }

    private String run(Path workingDirectory, int exitCode, String... arguments) throws Exception {
        final Process process = this.start(workingDirectory, exitCode, arguments);
        final String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        assertThat(process.waitFor()).isEqualTo(exitCode);
        return output;
    }

    private Process start(Path workingDirectory, int exitCode, String... arguments) throws Exception {
        if (!Files.exists(this.temp.resolve("launchers/wp"))) {
            this.writeLauncher(true);
        }
        final List<String> command = new ArrayList<>(List.of(this.temp.resolve("launchers/wp").toString()));
        command.addAll(List.of(arguments));
        final ProcessBuilder builder = new ProcessBuilder(command).directory(workingDirectory.toFile());
        builder.environment().put("PATH", this.bin + ":" + System.getenv("PATH"));
        builder.environment().put("DDEV_LOG", this.log.toString());
        builder.environment().put("PROJECT_ROOT", this.projectRoot.toString());
        builder.environment().put("FAKE_EXIT", String.valueOf(exitCode));
        return builder.start();
    }

    private void writeLauncher(boolean quietFlag) throws Exception {
        LauncherWriter.write(this.temp.resolve("launchers"), LauncherWriter.Target.UNIX, "site", this.projectRoot.toString(),
                this.bin.resolve("ddev").toString(), List.of(new ExposedCommands.Launcher("wp", "wp")), quietFlag);
    }

    private List<String> recorded() throws Exception {
        return List.of(Files.readString(this.log).split("\\|"));
    }

    private void dockerRunning(String names) throws Exception {
        executable(this.bin.resolve("docker"), "#!/usr/bin/env bash\nprintf '%s\\n' '" + names.replace("\n", "' '") + "'\n");
    }

    private static void executable(Path file, String content) throws Exception {
        Files.writeString(file, content);
        Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
    }
}
