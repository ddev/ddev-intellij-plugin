package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ServiceTest {
    @Test
    void prefersHttpsUrl() {
        final Service service = new Service("ddev-example-solr", "https://example.ddev.site:8984", "http://example.ddev.site:8983");

        assertThat(service.getPreferredUrl()).isEqualTo("https://example.ddev.site:8984");
    }

    @Test
    void fallsBackToHttpUrl() {
        final Service service = new Service("ddev-example-solr", null, "http://example.ddev.site:8983");

        assertThat(service.getPreferredUrl()).isEqualTo("http://example.ddev.site:8983");
    }

    @Test
    void hasNoPreferredUrlWithoutExposedUrls() {
        final Service service = new Service("ddev-example-worker", null, null);

        assertThat(service.getPreferredUrl()).isNull();
    }

    @Test
    void ignoresBlankUrls() {
        assertThat(new Service("ddev-example-solr", "", "http://example.ddev.site:8983").getPreferredUrl())
                .isEqualTo("http://example.ddev.site:8983");
        assertThat(new Service("ddev-example-worker", " ", "").getPreferredUrl()).isNull();
    }
}
