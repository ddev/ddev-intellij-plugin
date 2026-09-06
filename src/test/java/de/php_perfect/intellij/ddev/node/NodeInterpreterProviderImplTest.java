package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreterType;
import com.intellij.openapi.project.Project;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.nodejs.remote.NodeJSRemoteInterpreterManager;
import com.jetbrains.nodejs.remote.NodeJSRemoteSdkAdditionalData;
import com.jetbrains.nodejs.remote.NodeRemoteInterpreters;
import de.php_perfect.intellij.ddev.docker_compose.DockerComposeCredentialProvider;
import de.php_perfect.intellij.ddev.index.ManagedConfigurationIndex;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

final class NodeInterpreterProviderImplTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        ManagedConfigurationIndex.getInstance(getProject()).remove(NodeInterpreterConfig.class);
        super.tearDown();
    }

    @Test
    void registersEachProjectOnceAndPreservesAnExplicitInterpreterSelection() throws Exception {
        final Project project = mock(Project.class);
        final var index = ManagedConfigurationIndex.getInstance(getProject());
        when(project.getService(ManagedConfigurationIndex.class)).thenReturn(index);
        final var manager = mock(NodeJsInterpreterManager.class);
        when(project.getService(NodeJsInterpreterManager.class)).thenReturn(manager);
        when(manager.getInterpreterRef()).thenReturn(NodeJsLocalInterpreterType.createNodeFromPathRef());
        final var registry = mock(NodeRemoteInterpreters.class);
        final var unrelated = mock(NodeJSRemoteSdkAdditionalData.class);
        final var interpreters = new ArrayList<>(List.of(unrelated));
        when(registry.getInterpreters()).thenReturn(interpreters);
        doAnswer(invocation -> interpreters.add(invocation.getArgument(0))).when(registry).add(any());
        final var credentialProvider = mock(DockerComposeCredentialProvider.class);
        final var credentials = DockerComposeCredentialsType.getInstance().createCredentials();
        credentials.setComposeFilePaths(List.of("/second/.ddev/docker-compose.yaml"));
        credentials.setComposeServiceName("web");
        when(credentialProvider.getDdevDockerComposeCredentials(any())).thenReturn(credentials);
        final var remoteManager = mock(NodeJSRemoteInterpreterManager.class);
        try (var globalRegistry = mockStatic(NodeRemoteInterpreters.class);
             var globalCredentials = mockStatic(DockerComposeCredentialProvider.class);
             var globalMappings = mockStatic(NodeJSRemoteInterpreterManager.class)) {
            globalRegistry.when(NodeRemoteInterpreters::getInstance).thenReturn(registry);
            globalCredentials.when(DockerComposeCredentialProvider::getInstance).thenReturn(credentialProvider);
            globalMappings.when(NodeJSRemoteInterpreterManager::getInstance).thenReturn(remoteManager);
            final var provider = new NodeInterpreterProviderImpl(project);
            final var config = new NodeInterpreterConfig("second", "/second/.ddev/docker-compose.yaml", "/usr/bin/node");
            provider.configureNodeInterpreter(config);
            assertThat(interpreters).hasSize(2);
            final var managed = interpreters.getLast();
            verify(manager).setInterpreterRef(NodeJsInterpreterRef.create(managed.getSdkId()));
            assertThat(index.get(NodeInterpreterConfig.class).id()).isEqualTo(managed.getSdkId());

            provider.configureNodeInterpreter(config);
            verify(registry, times(1)).add(any());
            verify(remoteManager, times(1)).setupMappings(project, managed);

            clearInvocations(manager);
            when(manager.getInterpreterRef()).thenReturn(NodeJsInterpreterRef.create("/custom/node"));
            provider.configureNodeInterpreter(new NodeInterpreterConfig("second", config.composeFilePath(), "/opt/node"));
            assertThat(managed.getInterpreterPath()).isEqualTo("/opt/node");
            verify(manager, never()).setInterpreterRef(any());
            verify(registry, times(1)).add(any());

            // A pre-index installation is reused, rather than duplicated after upgrading the plugin.
            index.remove(NodeInterpreterConfig.class);
            provider.configureNodeInterpreter(config);
            assertThat(interpreters).hasSize(2);
            assertThat(index.get(NodeInterpreterConfig.class).id()).isEqualTo(managed.getSdkId());
            verify(unrelated, never()).setInterpreterPath(anyString());
        }
    }
}
