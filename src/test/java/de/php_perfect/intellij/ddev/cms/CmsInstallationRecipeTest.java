package de.php_perfect.intellij.ddev.cms;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

final class CmsInstallationRecipeTest {
    @Test
    void resolvesWordPressInstallationWithoutShellInterpolation() {
        final CmsInstallationRecipe recipe = CmsInstallationRecipe.forProjectType("wordpress");
        final CmsInstallationRecipe.Step install = recipe.steps().stream()
                .filter(step -> step.arguments().size() > 2
                        && step.arguments().subList(0, 3).equals(java.util.List.of("wp", "core", "install")))
                .findFirst().orElseThrow();

        assertThat(recipe.resolveArguments(install, new CmsInstallationRecipe.Context(
                "example", "https://example.ddev.site", "admin user", "p$ ss'word", "a@example.com")))
                .containsExactly("wp", "core", "install", "--url=https://example.ddev.site",
                        "--title=example", "--admin_user=admin user", "--admin_password=p$ ss'word",
                        "--admin_email=a@example.com");
    }

    @Test
    void exposesOnlyMaintainedRecipes() {
        assertThat(CmsInstallationRecipe.byId("astro")).isNotNull();
        assertThat(CmsInstallationRecipe.forProjectType("wordpress")).isNotNull();
        assertThat(CmsInstallationRecipe.forProjectType("drupal11")).isNotNull();
        assertThat(CmsInstallationRecipe.forProjectType("typo3")).isNotNull();
        assertThat(CmsInstallationRecipe.forProjectType("laravel")).isNotNull();
        assertThat(CmsInstallationRecipe.forProjectType("generic")).isNull();
        assertThat(CmsInstallationRecipe.all()).hasSizeGreaterThanOrEqualTo(15)
                .allSatisfy(recipe -> assertThat(recipe.steps().getFirst().arguments().getFirst())
                        .isEqualTo("start"));
        assertThat(CmsInstallationRecipe.byId("statamic").projectType()).isEqualTo("laravel");
    }

    @Test
    void resolvesOnlyUnambiguousProjectTypesForAutomaticInstallation() {
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("wordpress"))
                .extracting(CmsInstallationRecipe::id).isEqualTo("wordpress");
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("laravel"))
                .extracting(CmsInstallationRecipe::id).isEqualTo("laravel");
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("php")).isNull();
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("generic")).isNull();
    }

    @Test
    void catalogDeclaresItsEditorSchema() throws Exception {
        try (var stream = CmsInstallationRecipeTest.class.getResourceAsStream("/cms/recipes.json");
             var reader = new InputStreamReader(java.util.Objects.requireNonNull(stream),
                     StandardCharsets.UTF_8)) {
            assertThat(JsonParser.parseReader(reader).getAsJsonObject().get("$schema").getAsString())
                    .isEqualTo("./recipes.schema.json");
        }
        assertThat(CmsInstallationRecipeTest.class.getResource("/cms/recipes.schema.json")).isNotNull();
    }

    @Test
    void resolvesGeneratedProjectFiles() {
        final CmsInstallationRecipe recipe = CmsInstallationRecipe.byId("astro");
        final CmsInstallationRecipe.Step configure = recipe.steps().stream()
                .filter(step -> !step.fileActions().isEmpty()
                        && step.fileActions().stream().anyMatch(CmsInstallationRecipe.WriteFile.class::isInstance))
                .findFirst().orElseThrow();

        assertThat(recipe.resolveFileActions(configure, new CmsInstallationRecipe.Context(
                "astro-site", "https://astro-site.ddev.site", "", "", "")))
                .filteredOn(CmsInstallationRecipe.WriteFile.class::isInstance)
                .map(CmsInstallationRecipe.WriteFile.class::cast)
                .extracting(CmsInstallationRecipe.WriteFile::content)
                .anySatisfy(content -> assertThat(content)
                        .contains("site: \"https://astro-site.ddev.site\""));
    }

    @Test
    void resolvesProjectSpecificFirstLaunchUrls() {
        assertThat(CmsInstallationRecipe.byId("wordpress").firstLaunchUrl(
                "https://wordpress.ddev.site", "admin user"))
                .isEqualTo("https://wordpress.ddev.site/?ddev_intellij_login=admin+user");
        assertThat(CmsInstallationRecipe.byId("astro").firstLaunchUrl(
                "https://astro.ddev.site", ""))
                .isEqualTo("https://astro.ddev.site:4321");
        assertThat(CmsInstallationRecipe.byId("craftcms").firstLaunchUrl(
                "https://craft.ddev.site/", ""))
                .isEqualTo("https://craft.ddev.site/admin");
    }
}
