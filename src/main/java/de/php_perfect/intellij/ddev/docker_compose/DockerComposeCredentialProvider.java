package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;

public interface DockerComposeCredentialProvider {
    DockerComposeCredentialsHolder getDdevDockerComposeCredentials(@NotNull DockerComposeConfig dockerComposeConfig);

    static DockerComposeCredentialProvider getInstance() {
        return ApplicationManager.getApplication().getService(DockerComposeCredentialProvider.class);
    }
}
