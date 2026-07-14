package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

final class DescriptionTest {
    private static final DatabaseInfo DATABASE_INFO = new DatabaseInfo(DatabaseInfo.Type.MARIADB, "10.11", 3306, "db", "db", "db", "db", 32768);

    @Test
    void exposesBuilderValues() {
        final Description description = complete().build();

        Assertions.assertEquals("acol", description.getName());
        Assertions.assertEquals("8.3", description.getPhpVersion());
        Assertions.assertEquals(Description.Status.RUNNING, description.getStatus());
        Assertions.assertEquals("https://acol.ddev.site:8026", description.getMailHogHttpsUrl());
        Assertions.assertEquals("http://acol.ddev.site:8025", description.getMailHogHttpUrl());
        Assertions.assertEquals("https://acol.ddev.site:8027", description.getMailpitHttpsUrl());
        Assertions.assertEquals("http://acol.ddev.site:8028", description.getMailpitHttpUrl());
        Assertions.assertEquals(DATABASE_INFO, description.getDatabaseInfo());
        Assertions.assertEquals("https://acol.ddev.site", description.getPrimaryUrl());
    }

    @Test
    void servicesIncludeMailers() {
        final Map<String, Service> services = complete().build().getServices();

        Assertions.assertEquals(new Service("ddev-acol-web", "https://acol.ddev.site", "http://acol.ddev.site"), services.get("web"));
        Assertions.assertEquals(new Service("ddev-acol-mailhog", "https://acol.ddev.site:8026", "http://acol.ddev.site:8025"), services.get("mailhog"));
        Assertions.assertEquals("ddev-acol-mailpit", services.get("mailpit").getFullName());
        Assertions.assertEquals("https://acol.ddev.site:8027", services.get("mailpit").getHttpsUrl());
    }

    @Test
    void mailersWithOnlyHttpUrlAreListed() {
        final Map<String, Service> services = Description.builder()
                .name("acol")
                .mailHogHttpUrl("http://acol.ddev.site:8025")
                .mailpitHttpUrl("http://acol.ddev.site:8028")
                .build()
                .getServices();

        Assertions.assertEquals(new Service("ddev-acol-mailhog", null, "http://acol.ddev.site:8025"), services.get("mailhog"));
        Assertions.assertEquals("ddev-acol-mailpit", services.get("mailpit").getFullName());
    }

    @Test
    void mapsHttpOnlyMailpitUrlToTheSyntheticService() {
        final Description description = Description.builder()
                .name("example")
                .mailpitHttpUrl("http://example.ddev.site:8025")
                .build();

        Assertions.assertEquals(new Service("ddev-example-mailpit", null, "http://example.ddev.site:8025"), description.getServices().get("mailpit"));
    }

    @Test
    void servicesWithoutMailersAreEmpty() {
        Assertions.assertTrue(Description.builder().build().getServices().isEmpty());
    }

    @Test
    void servicesMissingFromJsonAreEmpty() {
        final Description description = new Gson().fromJson("{\"name\":\"acol\"}", Description.class);

        Assertions.assertTrue(description.getServices().isEmpty());
    }

    @Test
    void equalDescriptions() {
        final Description description = complete().build();

        Assertions.assertEquals(description, description);
        Assertions.assertEquals(complete().build(), description);
        Assertions.assertEquals(complete().build().hashCode(), description.hashCode());
        Assertions.assertNotEquals(null, description);
        Assertions.assertNotEquals("acol", description);
    }

    @Test
    void descriptionsDifferingInOneFieldAreNotEqual() {
        final Description description = complete().build();
        final List<Description> variants = List.of(
                complete().name("other").build(),
                complete().phpVersion("7.4").build(),
                complete().status(Description.Status.STOPPED).build(),
                complete().mailHogHttpsUrl(null).build(),
                complete().mailHogHttpUrl(null).build(),
                complete().mailpitHttpsUrl("https://other").build(),
                complete().mailpitHttpUrl(null).build(),
                complete().services(Map.of()).build(),
                complete().databaseInfo(null).build(),
                complete().primaryUrl(null).build()
        );

        for (final Description variant : variants) {
            Assertions.assertNotEquals(description, variant, variant.toString());
        }
    }

    @Test
    void toStringListsFields() {
        final String string = complete().build().toString();

        Assertions.assertTrue(string.startsWith("Description{name='acol'"), string);
        Assertions.assertTrue(string.contains("primaryUrl='https://acol.ddev.site'"), string);
    }

    private static Description.Builder complete() {
        return Description.builder()
                .name("acol")
                .phpVersion("8.3")
                .status(Description.Status.RUNNING)
                .mailHogHttpsUrl("https://acol.ddev.site:8026")
                .mailHogHttpUrl("http://acol.ddev.site:8025")
                .mailpitHttpsUrl("https://acol.ddev.site:8027")
                .mailpitHttpUrl("http://acol.ddev.site:8028")
                .services(Map.of("web", new Service("ddev-acol-web", "https://acol.ddev.site", "http://acol.ddev.site")))
                .databaseInfo(DATABASE_INFO)
                .primaryUrl("https://acol.ddev.site");
    }
}
