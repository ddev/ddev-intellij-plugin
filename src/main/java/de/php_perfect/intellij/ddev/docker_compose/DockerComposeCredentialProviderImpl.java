package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.docker.DockerCloudType;
import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.docker.remote.DockerCredentialsEditor;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public final class DockerComposeCredentialProviderImpl implements DockerComposeCredentialProvider {
    private static final String DOCKER_NAME = "Docker";
    private static final String SERVICE_NAME = "web";
    private static final String COMPOSE_PROJECT_NAME_ENV = "COMPOSE_PROJECT_NAME";
    private static final @NotNull Logger LOG = Logger.getInstance(DockerComposeCredentialProviderImpl.class);

    public DockerComposeCredentialsHolder getDdevDockerComposeCredentials(@NotNull DockerComposeConfig dockerComposeConfig) {
        this.ensureDockerRemoteServer();

        LOG.debug("Providing new docker credentials");

        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialsType.getInstance().createCredentials();
        credentials.setAccountName(DOCKER_NAME);
        credentials.setComposeFilePaths(dockerComposeConfig.composeFilePaths());
        credentials.setComposeServiceName(SERVICE_NAME);
        credentials.setRemoteProjectPath(DockerCredentialsEditor.DEFAULT_DOCKER_PROJECT_PATH);
        credentials.setEnvs(EnvironmentVariablesData.create(Map.of(COMPOSE_PROJECT_NAME_ENV, "ddev-" + dockerComposeConfig.projectName().replace(".", "").toLowerCase()), true));

        return credentials;
    }

    private void ensureDockerRemoteServer() {
        final var type = DockerCloudType.getInstance();
        final var remoteServerManager = RemoteServersManager.getInstance();

        if (remoteServerManager.findByName(DOCKER_NAME, type) == null) {
            remoteServerManager.addServer(remoteServerManager.createServer(type, DOCKER_NAME));
        }
    }
}
