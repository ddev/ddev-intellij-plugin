package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class AddOnListParserTest {
    @Test
    void parsesCurrentDdevTableEnvelopeAndWrappedDescriptions() {
        final String output = """
                {"level":"info","msg":"┌──┬──┐\\n│ ADD-ON │ DESCRIPTION │\\n├──┼──┤\\n│ ddev/ddev-redis │ Redis service │\\n├──┼──┤\\n│ vendor/ddev-tool │ A longer │\\n│                  │ description │\\n└──┴──┘"}
                """;

        assertThat(AddOnListParser.parse(output))
                .extracting(AddOn::getTitle, AddOn::getDescription, AddOn::getType)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("ddev/ddev-redis", "Redis service", "official"),
                        org.assertj.core.groups.Tuple.tuple("vendor/ddev-tool", "A longer description", "contrib")
                );
    }

    @Test
    void ignoresUnrelatedJsonLogLines() {
        assertThat(AddOnListParser.parse("{\"level\":\"warning\",\"msg\":\"Nothing to parse\"}"))
                .isEmpty();
    }
}
