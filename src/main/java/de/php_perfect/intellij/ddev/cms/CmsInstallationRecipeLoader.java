package de.php_perfect.intellij.ddev.cms;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CmsInstallationRecipeLoader {
    private static final Type CATALOG_TYPE = new TypeToken<CatalogDefinition>() { }.getType();
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{[A-Z_]+}");
    private static final Set<String> KNOWN_PLACEHOLDERS = Set.of(
            CmsInstallationRecipe.PRIMARY_URL,
            CmsInstallationRecipe.PROJECT_NAME,
            CmsInstallationRecipe.USERNAME,
            CmsInstallationRecipe.USERNAME_URLENCODED,
            CmsInstallationRecipe.LOGIN_TOKEN,
            CmsInstallationRecipe.LOGIN_TOKEN_URLENCODED,
            CmsInstallationRecipe.PASSWORD,
            CmsInstallationRecipe.EMAIL
    );

    private CmsInstallationRecipeLoader() {
    }

    static @NotNull Map<String, CmsInstallationRecipe> load(@NotNull String catalogResource) {
        try (InputStream stream = resource(catalogResource);
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            final CatalogDefinition catalog = new Gson().fromJson(reader, CATALOG_TYPE);
            final List<RecipeDefinition> definitions = catalog == null ? List.of() : list(catalog.recipes());
            if (definitions == null || definitions.isEmpty()) {
                throw new IllegalStateException("CMS recipe catalog is empty: " + catalogResource);
            }

            final Map<String, CmsInstallationRecipe> recipes = new LinkedHashMap<>();
            for (RecipeDefinition definition : definitions) {
                final CmsInstallationRecipe recipe = convert(definition);
                if (recipes.putIfAbsent(recipe.id(), recipe) != null) {
                    throw new IllegalStateException("Duplicate CMS recipe id: " + recipe.id());
                }
            }
            final Map<String, Long> automaticTypes = recipes.values().stream()
                    .filter(CmsInstallationRecipe::automaticProjectType)
                    .collect(java.util.stream.Collectors.groupingBy(CmsInstallationRecipe::projectType,
                            java.util.stream.Collectors.counting()));
            automaticTypes.forEach((projectType, count) -> {
                if (count > 1) {
                    throw new IllegalStateException("Multiple automatic CMS recipes for project type: " + projectType);
                }
            });
            return Map.copyOf(recipes);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read CMS recipe catalog " + catalogResource, exception);
        }
    }

    private static @NotNull CmsInstallationRecipe convert(@NotNull RecipeDefinition definition) {
        requireText(definition.id(), "id");
        requireText(definition.displayName(), "displayName", definition.id());
        requireText(definition.projectType(), "projectType", definition.id());

        final List<CmsInstallationRecipe.Step> steps = list(definition.steps()).stream()
                .map(step -> convertStep(definition.id(), step))
                .toList();
        if (steps.isEmpty()) {
            throw new IllegalStateException("CMS recipe has no steps: " + definition.id());
        }

        final List<String> configArguments = List.copyOf(list(definition.configArguments()));
        validateStrings(configArguments, "configArguments", definition.id());
        final LaunchDefinition launch = definition.launch();
        if (launch != null) {
            if (launch.port() != null && (launch.port() < 1 || launch.port() > 65_535)) {
                throw new IllegalStateException("Invalid launch port in CMS recipe " + definition.id());
            }
            validatePlaceholders(launch.query(), "launch.query", definition.id());
        }
        return new CmsInstallationRecipe(definition.id(), definition.displayName(), definition.projectType(),
                configArguments, steps, definition.requiresCredentials(),
                definition.automaticProjectType(), launch == null
                ? CmsInstallationRecipe.Launch.DEFAULT
                : new CmsInstallationRecipe.Launch(launch.path(), launch.port(), launch.query()));
    }

    private static @NotNull CmsInstallationRecipe.Step convertStep(@NotNull String recipeId,
                                                                    @NotNull StepDefinition definition) {
        requireText(definition.title(), "step title", recipeId);
        final List<CmsInstallationRecipe.FileAction> actions = new ArrayList<>();
        for (WriteDefinition write : list(definition.writeFiles())) {
            requireText(write.path(), "writeFiles.path", recipeId);
            requireText(write.resource(), "writeFiles.resource", recipeId);
            final String content = readText(write.resource());
            validatePlaceholders(content, "template " + write.resource(), recipeId);
            actions.add(new CmsInstallationRecipe.WriteFile(write.path(), content));
        }
        for (String directory : list(definition.moveContents())) {
            requireText(directory, "moveContents", recipeId);
            actions.add(new CmsInstallationRecipe.MoveContents(directory));
        }
        final List<String> arguments = List.copyOf(list(definition.arguments()));
        validateStrings(arguments, "step arguments", recipeId);
        if (!definition.sensitive() && arguments.stream().anyMatch(argument ->
                argument.contains(CmsInstallationRecipe.PASSWORD))) {
            throw new IllegalStateException("Password-bearing step must use protected standard input in CMS recipe "
                    + recipeId + ": " + definition.title());
        }
        if (arguments.isEmpty() && actions.isEmpty()) {
            throw new IllegalStateException("Empty step in CMS recipe " + recipeId + ": " + definition.title());
        }
        return new CmsInstallationRecipe.Step(definition.title(), arguments,
                List.copyOf(actions), definition.failureHelp(), definition.sensitive());
    }

    private static @NotNull String readText(@NotNull String path) {
        try (InputStream stream = resource(path)) {
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read CMS recipe resource " + path, exception);
        }
    }

    private static @NotNull InputStream resource(@NotNull String path) {
        final InputStream stream = CmsInstallationRecipeLoader.class.getResourceAsStream(path);
        if (stream == null) {
            throw new IllegalStateException("Missing CMS recipe resource: " + path);
        }
        return stream;
    }

    private static void requireText(@Nullable String value, @NotNull String field, @NotNull String... recipeId) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing " + field
                    + (recipeId.length == 0 ? "" : " in CMS recipe " + recipeId[0]));
        }
    }

    private static void validateStrings(@NotNull List<String> values, @NotNull String field,
                                        @NotNull String recipeId) {
        for (String value : values) {
            requireText(value, field, recipeId);
            validatePlaceholders(value, field, recipeId);
        }
    }

    private static void validatePlaceholders(@Nullable String value, @NotNull String field,
                                             @NotNull String recipeId) {
        if (value == null) {
            return;
        }
        final Matcher matcher = PLACEHOLDER.matcher(value);
        while (matcher.find()) {
            if (!KNOWN_PLACEHOLDERS.contains(matcher.group())) {
                throw new IllegalStateException("Unknown placeholder " + matcher.group() + " in " + field
                        + " of CMS recipe " + recipeId);
            }
        }
    }

    private static <T> @NotNull List<T> list(@Nullable List<T> values) {
        return values == null ? List.of() : values;
    }

    private record CatalogDefinition(@Nullable List<RecipeDefinition> recipes) {
    }

    private record RecipeDefinition(@Nullable String id, @Nullable String displayName,
                                    @Nullable String projectType, @Nullable List<String> configArguments,
                                    @Nullable List<StepDefinition> steps, boolean requiresCredentials,
                                    boolean automaticProjectType, @Nullable LaunchDefinition launch) {
    }

    private record StepDefinition(@Nullable String title, @Nullable List<String> arguments,
                                  @Nullable List<WriteDefinition> writeFiles,
                                  @Nullable List<String> moveContents, @Nullable String failureHelp,
                                  boolean sensitive) {
    }

    private record WriteDefinition(@Nullable String path, @Nullable String resource) {
    }

    private record LaunchDefinition(@Nullable String path, @Nullable Integer port, @Nullable String query) {
    }
}
