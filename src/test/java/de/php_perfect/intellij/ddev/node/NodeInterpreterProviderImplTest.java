package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

final class NodeInterpreterProviderImplTest extends BasePlatformTestCase {
    private NodeJsInterpreterRef originalInterpreterRef;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.originalInterpreterRef = NodeJsInterpreterManager.getInstance(this.getProject()).getInterpreterRef();
        this.removeRemoteInterpreters();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.removeRemoteInterpreters();
            ManagedConfigurationIndex.getInstance(this.getProject()).remove(NodeInterpreterConfig.class);
            NodeJsInterpreterManager.getInstance(this.getProject()).setInterpreterRef(this.originalInterpreterRef);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void createsRemoteInterpreter() {
        new NodeInterpreterProviderImpl(this.getProject()).configureNodeInterpreter(new NodeInterpreterConfig("acol", "/compose.yaml", "node"));

        final List<NodeJSRemoteSdkAdditionalData> interpreters = NodeRemoteInterpreters.getInstance().getInterpreters();
        Assertions.assertEquals(1, interpreters.size());

        final NodeJSRemoteSdkAdditionalData sdkData = interpreters.get(0);
        Assertions.assertEquals("node", sdkData.getInterpreterPath());
        Assertions.assertEquals(NodeInterpreterProviderImpl.NODEJS_HELPERS_PATH, sdkData.getHelpersPath());

        final Object credentials = sdkData.connectionCredentials().getCredentials();
        Assertions.assertInstanceOf(DockerComposeCredentialsHolder.class, credentials);
        Assertions.assertEquals(List.of("/compose.yaml"), ((DockerComposeCredentialsHolder) credentials).getComposeFilePaths());
        Assertions.assertSame(DockerComposeCredentialsType.getInstance(), sdkData.getRemoteConnectionType());

        Assertions.assertEquals(sdkData.getSdkId(), NodeJsInterpreterManager.getInstance(this.getProject()).getInterpreterRef().getReferenceName());
    }

    @Test
    void managedInterpreterIsUpdatedInPlace() {
        final NodeInterpreterProviderImpl provider = new NodeInterpreterProviderImpl(this.getProject());
        provider.configureNodeInterpreter(new NodeInterpreterConfig("acol", "/compose.yaml", "node"));

        provider.configureNodeInterpreter(new NodeInterpreterConfig("other", "/other.yaml", "nodejs"));

        final List<NodeJSRemoteSdkAdditionalData> interpreters = NodeRemoteInterpreters.getInstance().getInterpreters();
        Assertions.assertEquals(1, interpreters.size());
        final NodeJSRemoteSdkAdditionalData sdkData = interpreters.get(0);
        Assertions.assertEquals("nodejs", sdkData.getInterpreterPath());
        Assertions.assertEquals(List.of("/other.yaml"), sdkData.connectionCredentials().getCredentials(DockerComposeCredentialsType.getInstance()).getComposeFilePaths());
        Assertions.assertEquals(sdkData.getSdkId(), NodeJsInterpreterManager.getInstance(this.getProject()).getInterpreterRef().getReferenceName());
    }

    @Test
    void registersEachProjectOnceAndPreservesAnExplicitInterpreterSelection() throws Exception {
        final Project project = Mockito.mock(Project.class);
        final ManagedConfigurationIndex index = ManagedConfigurationIndex.getInstance(this.getProject());
        Mockito.when(project.getService(ManagedConfigurationIndex.class)).thenReturn(index);
        final NodeJsInterpreterManager manager = Mockito.mock(NodeJsInterpreterManager.class);
        Mockito.when(project.getService(NodeJsInterpreterManager.class)).thenReturn(manager);
        Mockito.when(manager.getInterpreterRef()).thenReturn(NodeJsLocalInterpreterType.createNodeFromPathRef());
        final NodeRemoteInterpreters registry = Mockito.mock(NodeRemoteInterpreters.class);
        final NodeJSRemoteSdkAdditionalData unrelated = Mockito.mock(NodeJSRemoteSdkAdditionalData.class);
        final List<NodeJSRemoteSdkAdditionalData> interpreters = new ArrayList<>(List.of(unrelated));
        Mockito.when(registry.getInterpreters()).thenReturn(interpreters);
        Mockito.doAnswer(invocation -> interpreters.add(invocation.getArgument(0))).when(registry).add(Mockito.any());
        final DockerComposeCredentialProvider credentialProvider = Mockito.mock(DockerComposeCredentialProvider.class);
        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialsType.getInstance().createCredentials();
        credentials.setComposeFilePaths(List.of("/second/.ddev/docker-compose.yaml"));
        credentials.setComposeServiceName("web");
        Mockito.when(credentialProvider.getDdevDockerComposeCredentials(Mockito.any())).thenReturn(credentials);
        final NodeJSRemoteInterpreterManager remoteManager = Mockito.mock(NodeJSRemoteInterpreterManager.class);

        try (MockedStatic<NodeRemoteInterpreters> globalRegistry = Mockito.mockStatic(NodeRemoteInterpreters.class);
             MockedStatic<DockerComposeCredentialProvider> globalCredentials = Mockito.mockStatic(DockerComposeCredentialProvider.class);
             MockedStatic<NodeJSRemoteInterpreterManager> globalMappings = Mockito.mockStatic(NodeJSRemoteInterpreterManager.class)) {
            globalRegistry.when(NodeRemoteInterpreters::getInstance).thenReturn(registry);
            globalCredentials.when(DockerComposeCredentialProvider::getInstance).thenReturn(credentialProvider);
            globalMappings.when(NodeJSRemoteInterpreterManager::getInstance).thenReturn(remoteManager);
            final NodeInterpreterProviderImpl provider = new NodeInterpreterProviderImpl(project);
            final NodeInterpreterConfig config = new NodeInterpreterConfig("second", "/second/.ddev/docker-compose.yaml", "/usr/bin/node");

            provider.configureNodeInterpreter(config);
            Assertions.assertEquals(2, interpreters.size());
            final NodeJSRemoteSdkAdditionalData managed = interpreters.getLast();
            Mockito.verify(manager).setInterpreterRef(NodeJsInterpreterRef.create(managed.getSdkId()));
            Assertions.assertEquals(managed.getSdkId(), index.get(NodeInterpreterConfig.class).id());

            provider.configureNodeInterpreter(config);
            Mockito.verify(registry, Mockito.times(1)).add(Mockito.any());
            Mockito.verify(remoteManager, Mockito.times(1)).setupMappings(project, managed);

            Mockito.clearInvocations(manager);
            Mockito.when(manager.getInterpreterRef()).thenReturn(NodeJsInterpreterRef.create("/custom/node"));
            provider.configureNodeInterpreter(new NodeInterpreterConfig("second", config.composeFilePath(), "/opt/node"));
            Assertions.assertEquals("/opt/node", managed.getInterpreterPath());
            Mockito.verify(manager, Mockito.never()).setInterpreterRef(Mockito.any());
            Mockito.verify(registry, Mockito.times(1)).add(Mockito.any());

            // Unindexed matching interpreters are reused, not duplicated.
            index.remove(NodeInterpreterConfig.class);
            provider.configureNodeInterpreter(config);
            Assertions.assertEquals(2, interpreters.size());
            Assertions.assertEquals(managed.getSdkId(), index.get(NodeInterpreterConfig.class).id());
            Mockito.verify(unrelated, Mockito.never()).setInterpreterPath(Mockito.anyString());
        }
    }

    private void removeRemoteInterpreters() {
        final NodeRemoteInterpreters remoteInterpreters = NodeRemoteInterpreters.getInstance();
        for (NodeJSRemoteSdkAdditionalData sdkData : List.copyOf(remoteInterpreters.getInterpreters())) {
            remoteInterpreters.delete(sdkData.getSdkId());
        }
    }
}
