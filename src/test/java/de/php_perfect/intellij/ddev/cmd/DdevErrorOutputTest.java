package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DdevErrorOutputTest {
    @Test
    void extractsFatalMessageFromJsonOutput() {
        final String stderr = """
                {"level":"warning","msg":"Docker is slow","time":"2026-09-26T19:45:49+02:00"}
                {"level":"fatal","msg":"a project (web container) in running state already exists","time":"2026-09-26T19:45:50+02:00"}
                """;

        Assertions.assertEquals("a project (web container) in running state already exists", DdevErrorOutput.extractMessage(stderr));
    }

    @Test
    void usesPlainTextOutput() {
        Assertions.assertEquals("zsh:1: no such file or directory: /usr/local/bin/ddev",
                DdevErrorOutput.extractMessage("zsh:1: no such file or directory: /usr/local/bin/ddev\n"));
    }

    @Test
    void ignoresNonErrorJsonLines() {
        Assertions.assertNull(DdevErrorOutput.extractMessage("{\"level\":\"info\",\"msg\":\"Starting\"}\n"));
    }

    @Test
    void emptyOutput() {
        Assertions.assertNull(DdevErrorOutput.extractMessage(""));
    }
}
