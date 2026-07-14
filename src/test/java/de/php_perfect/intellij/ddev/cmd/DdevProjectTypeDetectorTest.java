package de.php_perfect.intellij.ddev.cmd;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

final class DdevProjectTypeDetectorTest {
    @TempDir
    Path directory;

    @Test
    void readsQuotedTopLevelType() throws Exception {
        Files.createDirectories(this.directory.resolve(".ddev"));
        Files.writeString(this.directory.resolve(".ddev/config.yaml"),
                "name: example\ntype: 'wordpress' # comment\ndocroot: ''\n");

        assertThat(DdevProjectTypeDetector.detect(this.directory.toString())).isEqualTo("wordpress");
    }

    @Test
    void returnsNullWithoutConfig() {
        assertThat(DdevProjectTypeDetector.detect(this.directory.toString())).isNull();
    }
}
