package de.php_perfect.intellij.ddev.cms;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

final class CmsInstallationRecipeTest {
    @Test
    void resolvesWordPressInstallationWithoutShellInterpolation() {
        final CmsInstallationRecipe recipe = CmsInstallationRecipe.byId("wordpress");
        final CmsInstallationRecipe.Step install = recipe.steps().stream()
                .filter(step -> step.arguments().size() > 2
                        && step.arguments().subList(0, 3).equals(java.util.List.of("wp", "core", "install")))
                .findFirst().orElseThrow();

        assertThat(recipe.resolveArguments(install, new CmsInstallationRecipe.Context(
                "example", "https://example.ddev.site", "admin user", "p$ ss'word", "a@example.com", "token")))
                .containsExactly("wp", "core", "install", "--url=https://example.ddev.site",
                        "--title=example", "--admin_user=admin user", "--admin_password=p$ ss'word",
                        "--admin_email=a@example.com");
    }

    @Test
    void exposesOnlyMaintainedRecipes() {
        assertThat(CmsInstallationRecipe.byId("astro")).isNotNull();
        assertThat(CmsInstallationRecipe.byId("wordpress")).isNotNull();
        assertThat(CmsInstallationRecipe.byId("drupal11")).isNotNull();
        assertThat(CmsInstallationRecipe.byId("typo3")).isNotNull();
        assertThat(CmsInstallationRecipe.byId("laravel")).isNotNull();
        assertThat(CmsInstallationRecipe.byId("generic")).isNull();
        assertThat(CmsInstallationRecipe.all()).hasSizeGreaterThanOrEqualTo(15)
                .allSatisfy(recipe -> assertThat(recipe.steps().getFirst().arguments().getFirst())
                        .isEqualTo("start"));
        assertThat(CmsInstallationRecipe.byId("statamic").projectType()).isEqualTo("laravel");
        assertThat(CmsInstallationRecipe.all()).flatExtracting(CmsInstallationRecipe::steps)
                .filteredOn(step -> step.arguments().stream()
                        .anyMatch(argument -> argument.contains(CmsInstallationRecipe.PASSWORD)))
                .allMatch(CmsInstallationRecipe.Step::sensitive);
    }

    @Test
    void resolvesOnlyUnambiguousProjectTypesForAutomaticInstallation() {
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("wordpress"))
                .extracting(CmsInstallationRecipe::id).isEqualTo("wordpress");
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("laravel"))
                .extracting(CmsInstallationRecipe::id).isEqualTo("laravel");
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("php")).isNull();
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("generic")).isNull();
        assertThat(CmsInstallationRecipe.forAutomaticProjectType("magento2")).isNull();
        assertThat(CmsInstallationRecipe.byId("magento2").steps())
                .extracting(CmsInstallationRecipe.Step::title)
                .startsWith("Start DDEV", "Check Magento Composer public key",
                        "Check Magento Composer private key", "Install OpenSearch");
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
                "astro-site", "https://astro-site.ddev.site", "", "", "", "")))
                .filteredOn(CmsInstallationRecipe.WriteFile.class::isInstance)
                .map(CmsInstallationRecipe.WriteFile.class::cast)
                .extracting(CmsInstallationRecipe.WriteFile::content)
                .anySatisfy(content -> assertThat(content)
                        .contains("site: \"https://astro-site.ddev.site\""));
    }

    @Test
    void embedsAnExpiringTokenInTheWordPressLoginHelper() {
        final CmsInstallationRecipe recipe = CmsInstallationRecipe.byId("wordpress");
        final CmsInstallationRecipe.Step login = recipe.steps().stream()
                .filter(step -> step.title().contains("one-time WordPress login"))
                .findFirst().orElseThrow();

        assertThat(recipe.resolveFileActions(login, new CmsInstallationRecipe.Context(
                "wordpress", "https://wordpress.ddev.site", "admin", "password",
                "admin@example.com", "0123456789abcdef")))
                .singleElement().asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.type(
                        CmsInstallationRecipe.WriteFile.class))
                .extracting(CmsInstallationRecipe.WriteFile::content)
                .asString()
                .contains("hash_equals( '0123456789abcdef', $token )")
                .contains("filemtime( __FILE__ ) + HOUR_IN_SECONDS")
                .doesNotContain("${LOGIN_TOKEN}");
    }

    @Test
    void resolvesProjectSpecificFirstLaunchUrls() {
        assertThat(CmsInstallationRecipe.byId("wordpress").firstLaunchUrl(
                "https://wordpress.ddev.site", "admin user", "secret token"))
                .isEqualTo("https://wordpress.ddev.site/?ddev_intellij_login=admin+user&ddev_intellij_token=secret+token");
        assertThat(CmsInstallationRecipe.byId("astro").firstLaunchUrl(
                "https://astro.ddev.site", "", ""))
                .isEqualTo("https://astro.ddev.site:4321");
        assertThat(CmsInstallationRecipe.byId("craftcms").firstLaunchUrl(
                "https://craft.ddev.site/", "", ""))
                .isEqualTo("https://craft.ddev.site/admin");
    }
}
