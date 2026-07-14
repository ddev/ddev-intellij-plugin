package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class DescriptionTest {
    @Test
    void mapsHttpOnlyMailpitUrlToTheSyntheticService() {
        final Description description = Description.builder()
                .name("example")
                .mailpitHttpUrl("http://example.ddev.site:8025")
                .build();

        assertThat(description.getServices().get("mailpit"))
                .isEqualTo(new Service("ddev-example-mailpit", null, "http://example.ddev.site:8025"));
    }
}
