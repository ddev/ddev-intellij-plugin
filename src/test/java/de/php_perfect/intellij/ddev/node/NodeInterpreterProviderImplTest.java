package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.nodejs.remote.NodeJSRemoteSdkAdditionalData;
import com.jetbrains.nodejs.remote.NodeRemoteInterpreters;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    void existingRemoteInterpreterIsKept() {
        final NodeInterpreterProviderImpl provider = new NodeInterpreterProviderImpl(this.getProject());
        provider.configureNodeInterpreter(new NodeInterpreterConfig("acol", "/compose.yaml", "node"));
        final String sdkId = NodeRemoteInterpreters.getInstance().getInterpreters().get(0).getSdkId();

        provider.configureNodeInterpreter(new NodeInterpreterConfig("other", "/other.yaml", "nodejs"));

        final List<NodeJSRemoteSdkAdditionalData> interpreters = NodeRemoteInterpreters.getInstance().getInterpreters();
        Assertions.assertEquals(1, interpreters.size());
        Assertions.assertEquals(sdkId, interpreters.get(0).getSdkId());
    }

    private void removeRemoteInterpreters() {
        final NodeRemoteInterpreters remoteInterpreters = NodeRemoteInterpreters.getInstance();
        for (NodeJSRemoteSdkAdditionalData sdkData : List.copyOf(remoteInterpreters.getInterpreters())) {
            remoteInterpreters.delete(sdkData.getSdkId());
        }
    }
}
