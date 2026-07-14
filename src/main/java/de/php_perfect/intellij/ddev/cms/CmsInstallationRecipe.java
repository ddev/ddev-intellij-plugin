package de.php_perfect.intellij.ddev.cms;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public record CmsInstallationRecipe(
        @NotNull String id,
        @NotNull String displayName,
        @NotNull String projectType,
        @NotNull List<String> configArguments,
        @NotNull List<Step> steps,
        boolean requiresCredentials,
        boolean automaticProjectType,
        @NotNull Launch launch
) {
    public static final String PRIMARY_URL = "${PRIMARY_URL}";
    public static final String PROJECT_NAME = "${PROJECT_NAME}";
    public static final String USERNAME = "${USERNAME}";
    public static final String PASSWORD = "${PASSWORD}";
    public static final String EMAIL = "${EMAIL}";
    public static final String USERNAME_URLENCODED = "${USERNAME_URLENCODED}";

    private static final Map<String, CmsInstallationRecipe> RECIPES =
            CmsInstallationRecipeLoader.load("/cms/recipes.json");

    public static @Nullable CmsInstallationRecipe forProjectType(@NotNull String projectType) {
        return RECIPES.get(projectType);
    }

    public static @Nullable CmsInstallationRecipe byId(@NotNull String id) {
        return RECIPES.get(id);
    }

    public static @Nullable CmsInstallationRecipe forAutomaticProjectType(@NotNull String projectType) {
        return RECIPES.values().stream()
                .filter(CmsInstallationRecipe::automaticProjectType)
                .filter(recipe -> recipe.projectType().equals(projectType))
                .findFirst().orElse(null);
    }

    public static @NotNull List<CmsInstallationRecipe> all() {
        return RECIPES.values().stream().sorted(Comparator.comparing(CmsInstallationRecipe::displayName)).toList();
    }

    public @NotNull String firstLaunchUrl(@NotNull String primaryUrl, @NotNull String username) {
        if (this.launch.equals(Launch.DEFAULT)) {
            return primaryUrl;
        }
        try {
            final URI primary = URI.create(primaryUrl);
            final String path = this.launch.path() == null ? primary.getPath() : this.launch.path();
            final String query = this.launch.query() == null ? primary.getRawQuery()
                    : this.launch.query().replace(USERNAME_URLENCODED,
                    URLEncoder.encode(username, StandardCharsets.UTF_8));
            final URI launchUri = new URI(primary.getScheme(), primary.getUserInfo(), primary.getHost(),
                    this.launch.port() == null ? primary.getPort() : this.launch.port(),
                    path, null, null);
            return query == null ? launchUri.toString() : launchUri + "?" + query;
        } catch (IllegalArgumentException | URISyntaxException ignored) {
            return primaryUrl;
        }
    }

    public @NotNull List<String> resolveArguments(@NotNull Step step, @NotNull Context context) {
        return step.arguments().stream().map(argument -> resolve(argument, context)).toList();
    }

    public @NotNull List<FileAction> resolveFileActions(@NotNull Step step, @NotNull Context context) {
        return step.fileActions().stream().map(action -> (FileAction) switch (action) {
            case WriteFile writeFile -> new WriteFile(writeFile.relativePath(), resolve(writeFile.content(), context));
            case MoveContents moveContents -> moveContents;
        }).toList();
    }

    private static @NotNull String resolve(@NotNull String value, @NotNull Context context) {
        return value.replace(PRIMARY_URL, context.primaryUrl())
                .replace(PROJECT_NAME, context.projectName())
                .replace(USERNAME, context.username())
                .replace(PASSWORD, context.password())
                .replace(EMAIL, context.email());
    }

    public record Step(@NotNull String title, @NotNull List<String> arguments,
                       @NotNull List<FileAction> fileActions) {
    }

    public sealed interface FileAction permits WriteFile, MoveContents {
    }

    public record WriteFile(@NotNull String relativePath, @NotNull String content) implements FileAction {
    }

    public record MoveContents(@NotNull String relativeDirectory) implements FileAction {
    }

    public record Context(@NotNull String projectName, @NotNull String primaryUrl,
                          @NotNull String username, @NotNull String password, @NotNull String email) {
    }

    public record Launch(@Nullable String path, @Nullable Integer port, @Nullable String query) {
        public static final Launch DEFAULT = new Launch(null, null, null);
    }
}
