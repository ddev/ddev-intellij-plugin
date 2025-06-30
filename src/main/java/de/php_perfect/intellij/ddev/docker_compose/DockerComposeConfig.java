package de.php_perfect.intellij.ddev.docker_compose;

import de.php_perfect.intellij.ddev.index.IndexableConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record DockerComposeConfig(@NotNull List<String> composeFilePaths,
                                  @NotNull String projectName) implements IndexableConfiguration {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DockerComposeConfig that = (DockerComposeConfig) o;
        return Objects.equals(composeFilePaths, that.composeFilePaths) && Objects.equals(projectName, that.projectName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(composeFilePaths, projectName);
    }

    @Override
    public String toString() {
        return "DockerComposeConfig{" +
                "composeFilePaths=" + composeFilePaths +
                ", projectName='" + projectName + '\'' +
                '}';
    }
}
