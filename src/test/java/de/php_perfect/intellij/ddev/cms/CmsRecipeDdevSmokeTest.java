package de.php_perfect.intellij.ddev.cms;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("ddev-smoke")
@EnabledIfEnvironmentVariable(named = "DDEV_SMOKE_TESTS", matches = "true")
final class CmsRecipeDdevSmokeTest {
    @TempDir
    Path projectRoot;
    private String projectName;
    private String primaryUrl;
    private CmsProjectInstaller.Credentials credentials;

    @AfterEach
    void removeDdevProject() throws Exception {
        if (this.projectName != null) {
            run(this.projectRoot, null, "ddev", "delete", "--yes", "--omit-snapshot");
        }
    }

    @Test
    void installsWordPressEndToEnd() throws Exception {
        install("wordpress");
        run(this.projectRoot, null, "ddev", "wp", "core", "is-installed");
        assertThat(this.projectRoot.resolve("wp-config.php")).isRegularFile();
        final Path loginHelper = this.projectRoot.resolve("wp-content/mu-plugins/ddev-intellij-login.php");
        assertThat(loginHelper).isRegularFile();

        run(this.projectRoot, null, "curl", "-sk", "-o", "/dev/null",
                this.primaryUrl + "/?ddev_intellij_login=admin&ddev_intellij_token=wrong");
        assertThat(loginHelper).isRegularFile();

        run(this.projectRoot, null, "curl", "-sk", "-o", "/dev/null",
                this.primaryUrl + "/?ddev_intellij_login=admin&ddev_intellij_token="
                        + this.credentials.loginToken());
        assertThat(loginHelper).doesNotExist();
    }

    @Test
    void installsLaravelEndToEnd() throws Exception {
        install("laravel");
        assertThat(this.projectRoot.resolve("artisan")).isRegularFile();
    }

    @Test
    void installsSharedPhpTypeRecipeEndToEnd() throws Exception {
        install("grav");
        assertThat(this.projectRoot.resolve("index.php")).isRegularFile();
    }

    private void install(String recipeId) throws Exception {
        final CmsInstallationRecipe recipe = java.util.Objects.requireNonNull(
                CmsInstallationRecipe.byId(recipeId));
        this.projectName = "ddev-plugin-smoke-" + UUID.randomUUID().toString().substring(0, 8);
        final List<String> config = new ArrayList<>(List.of("ddev", "config",
                "--project-name=" + this.projectName));
        config.addAll(recipe.configArguments());
        run(this.projectRoot, null, config.toArray(String[]::new));

        this.credentials = CmsProjectInstaller.Credentials.create(
                "admin", "DdevSmoke-" + UUID.randomUUID() + "!", "admin@example.com");
        this.primaryUrl = "";
        for (int index = 0; index < recipe.steps().size(); index++) {
            final CmsInstallationRecipe.Step step = recipe.steps().get(index);
            final CmsInstallationRecipe.Context context = new CmsInstallationRecipe.Context(
                    this.projectName, this.primaryUrl, this.credentials.username(), this.credentials.password(),
                    this.credentials.email(), this.credentials.loginToken());
            final List<String> arguments = recipe.resolveArguments(step, context);
            if (!arguments.isEmpty()) {
                if (step.sensitive()) {
                    final String script = "set -euo pipefail\nexec " + arguments.stream()
                            .map(CmsProjectInstaller::shellQuote)
                            .collect(java.util.stream.Collectors.joining(" ")) + "\n";
                    run(this.projectRoot, script, "ddev", "exec", "bash", "-s");
                } else {
                    final List<String> command = new ArrayList<>(List.of("ddev"));
                    command.addAll(arguments);
                    run(this.projectRoot, null, command.toArray(String[]::new));
                }
            }
            for (CmsInstallationRecipe.FileAction action : recipe.resolveFileActions(step, context)) {
                switch (action) {
                    case CmsInstallationRecipe.WriteFile write ->
                            CmsProjectInstaller.writeProjectFile(this.projectRoot, write);
                    case CmsInstallationRecipe.MoveContents move ->
                            CmsProjectInstaller.moveProjectContents(this.projectRoot, move.relativeDirectory());
                }
            }
            if (index == 0) {
                this.primaryUrl = describePrimaryUrl();
            }
        }
    }

    private String describePrimaryUrl() throws Exception {
        final String output = run(this.projectRoot, null, "ddev", "describe", "-j");
        for (String line : output.lines().toList()) {
            final var object = JsonParser.parseString(line).getAsJsonObject();
            if (object.has("raw")) {
                return object.getAsJsonObject("raw").get("primary_url").getAsString();
            }
        }
        throw new AssertionError("DDEV describe did not return a primary URL");
    }

    private static String run(Path directory, String standardInput, String... command) throws Exception {
        final Process process = new ProcessBuilder(command)
                .directory(directory.toFile())
                .redirectErrorStream(true)
                .start();
        final java.util.concurrent.CompletableFuture<String> outputFuture =
                java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                    try {
                        return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                    } catch (java.io.IOException exception) {
                        throw new java.io.UncheckedIOException(exception);
                    }
                });
        if (standardInput != null) {
            try (var input = process.getOutputStream()) {
                input.write(standardInput.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            process.getOutputStream().close();
        }
        final boolean completed = process.waitFor(Duration.ofMinutes(15).toMillis(),
                java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!completed) {
            process.destroyForcibly();
            throw new AssertionError("Timed out running " + String.join(" ", command));
        }
        final String output = outputFuture.join();
        assertThat(process.exitValue()).as("%s%n%s", String.join(" ", command), output).isZero();
        return output;
    }
}
