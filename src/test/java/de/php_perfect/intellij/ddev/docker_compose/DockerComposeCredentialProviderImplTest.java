package de.php_perfect.intellij.ddev.docker_compose;

import com.intellij.docker.DockerCloudType;
import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerCredentialsEditor;
import com.intellij.remoteServer.configuration.RemoteServer;
import com.intellij.remoteServer.configuration.RemoteServersManager;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

final class DockerComposeCredentialProviderImplTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void providesCredentialsForWebService() {
        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialProvider.getInstance()
                .getDdevDockerComposeCredentials(new DockerComposeConfig(List.of("/compose.yaml"), "My.Project"));

        Assertions.assertEquals("Docker", credentials.getAccountName());
        Assertions.assertEquals(List.of("/compose.yaml"), credentials.getComposeFilePaths());
        Assertions.assertEquals("web", credentials.getComposeServiceName());
        Assertions.assertEquals(DockerCredentialsEditor.DEFAULT_DOCKER_PROJECT_PATH, credentials.getRemoteProjectPath());
        Assertions.assertEquals(Map.of("COMPOSE_PROJECT_NAME", "ddev-myproject"), credentials.getEnvs().getEnvs());
    }

    @Test
    void createsMissingDockerServerOnce() {
        final RemoteServersManager serversManager = RemoteServersManager.getInstance();
        final RemoteServer<?> existingServer = serversManager.findByName("Docker", DockerCloudType.getInstance());
        if (existingServer != null) {
            serversManager.removeServer(existingServer);
        }

        final DockerComposeCredentialProvider provider = new DockerComposeCredentialProviderImpl();
        final DockerComposeConfig config = new DockerComposeConfig(List.of("/compose.yaml"), "acol");
        provider.getDdevDockerComposeCredentials(config);
        provider.getDdevDockerComposeCredentials(config);

        final long dockerServers = serversManager.getServers(DockerCloudType.getInstance()).stream()
                .filter(server -> server.getName().equals("Docker"))
                .count();
        Assertions.assertEquals(1, dockerServers);
    }
}
