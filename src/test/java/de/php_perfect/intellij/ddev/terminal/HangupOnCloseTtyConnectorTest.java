package de.php_perfect.intellij.ddev.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.assertj.core.api.Assertions.assertThat;

final class HangupOnCloseTtyConnectorTest {
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void aProcessWithoutHangupIsReportedSoItGetsClosedNormally() throws Exception {
        final Process process = new ProcessBuilder("sleep", "5").start();
        try {
            assertThat(HangupOnCloseTtyConnector.hangup(process)).isFalse();
            assertThat(process.isAlive()).isTrue();
        } finally {
            process.destroyForcibly();
        }
    }
}
