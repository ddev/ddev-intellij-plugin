package de.php_perfect.intellij.ddev.node;

import com.intellij.docker.remote.DockerComposeCredentialsHolder;
import com.intellij.docker.remote.DockerComposeCredentialsType;
import com.intellij.execution.ExecutionException;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager;
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathMappingSettings;
import com.jetbrains.nodejs.remote.NodeJSRemoteInterpreterManager;
import com.jetbrains.nodejs.remote.NodeJSRemoteSdkAdditionalData;
import com.jetbrains.nodejs.remote.NodeRemoteInterpreters;
import de.php_perfect.intellij.ddev.dockerCompose.DockerComposeConfig;
import de.php_perfect.intellij.ddev.dockerCompose.DockerComposeCredentialProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

public final class NodeInterpreterProviderImpl implements NodeInterpreterProvider {
    public static final @NotNull String NODEJS_HELPERS_PATH = ".webstorm_nodejs_helpers";
    private static final @NotNull Logger LOG = Logger.getInstance(NodeInterpreterProviderImpl.class);
    private final @NotNull Project project;

    public NodeInterpreterProviderImpl(final @NotNull Project project) {
        this.project = project;
    }

    /**
     * Configures a Node.js interpreter for the DDEV environment.
     * If an interpreter with matching compose file already exists, it will be reused.
     * Otherwise, a new interpreter will be created and configured.
     *
     * @param nodeInterpreterConfig Configuration for the Node.js interpreter
     */
    public void configureNodeInterpreter(final @NotNull NodeInterpreterConfig nodeInterpreterConfig) {
        final NodeRemoteInterpreters nodeRemoteInterpreters = NodeRemoteInterpreters.getInstance();
        final Collection<NodeJSRemoteSdkAdditionalData> interpreters = nodeRemoteInterpreters.getInterpreters();
        final String normalizedTargetPath = normalizePath(nodeInterpreterConfig.composeFilePath());

        // Check if we already have a matching remote interpreter set up
        if (!interpreters.isEmpty() && isInterpreterAlreadyConfigured(interpreters, normalizedTargetPath)) {
            LOG.debug("Found existing nodejs interpreter");
            return;
        }

        // Create and configure a new interpreter
        LOG.debug("Creating nodejs interpreter");

        // Create credentials for Docker Compose
        final DockerComposeCredentialsHolder credentials = DockerComposeCredentialProvider.getInstance()
            .getDdevDockerComposeCredentials(
                new DockerComposeConfig(List.of(nodeInterpreterConfig.composeFilePath()), nodeInterpreterConfig.name())
            );

        // Build and configure the SDK data
        final NodeJSRemoteSdkAdditionalData sdkData = buildNodeJSRemoteSdkAdditionalData(
            credentials, nodeInterpreterConfig.binaryPath()
        );

        // Register the new interpreter
        nodeRemoteInterpreters.add(sdkData);
        NodeJsInterpreterManager.getInstance(this.project)
            .setInterpreterRef(NodeJsInterpreterRef.create(sdkData.getSdkId()));
    }

    /**
     * @param interpreters Collection of existing interpreters
     * @param normalizedTargetPath Normalized path to match against
     * @return true if a matching interpreter exists, false otherwise
     */
    private boolean isInterpreterAlreadyConfigured(Collection<NodeJSRemoteSdkAdditionalData> interpreters, String normalizedTargetPath) {
        for (NodeJSRemoteSdkAdditionalData interpreter : interpreters) {
            Object credentialsObj = interpreter.connectionCredentials().getCredentials();

            if (credentialsObj instanceof DockerComposeCredentialsHolder credentials &&
                credentials.getComposeFilePaths() != null) {

                for (String composeFilePath : credentials.getComposeFilePaths()) {
                    String normalizedExistingPath = normalizePath(composeFilePath);
                    if (normalizedExistingPath.contains(normalizedTargetPath)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private @NotNull NodeJSRemoteSdkAdditionalData buildNodeJSRemoteSdkAdditionalData(DockerComposeCredentialsHolder credentials, @NotNull String binaryPath) {
        final NodeJSRemoteSdkAdditionalData sdkData = new NodeJSRemoteSdkAdditionalData(binaryPath);
        sdkData.setCredentials(DockerComposeCredentialsType.getInstance().getCredentialsKey(), credentials);
        sdkData.setHelpersPath(NODEJS_HELPERS_PATH);
        sdkData.setPathMappings(this.loadPathMappings(sdkData));

        return sdkData;
    }

    private PathMappingSettings loadPathMappings(NodeJSRemoteSdkAdditionalData sdkData) {
        final NodeJSRemoteInterpreterManager nodeRemoteInterpreterManager = NodeJSRemoteInterpreterManager.getInstance();

        try {
            return nodeRemoteInterpreterManager.setupMappings(this.project, sdkData);
        } catch (ExecutionException e) {
            return null;
        }
    }

    /**
     * Normalizes a file path by replacing backslashes with forward slashes
     * and ensuring consistent path separators for comparison.
     *
     * @param path The path to normalize
     * @return The normalized path
     */
    private String normalizePath(String path) {
        if (path == null) {
            return "";
        }
        // Replace backslashes with forward slashes for consistent comparison
        return path.replace('\\', '/');
    }
}
