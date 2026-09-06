package de.php_perfect.intellij.ddev.php.server;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.config.servers.PhpServer;
import com.jetbrains.php.config.servers.PhpServersWorkspaceStateComponent;
import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Objects;

final class ServerConfigManagerImplTest extends BasePlatformTestCase {

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
    void configure() throws URISyntaxException {
        final ServerConfig serverConfig = new ServerConfig(
                Objects.requireNonNull(this.getProject().getBasePath()),
                "/var/www/html",
                new URI("https://test.ddev.site")
        );

        final ServerConfigManager serverConfigManager = ServerConfigManager.getInstance(this.getProject());
        serverConfigManager.configure(serverConfig);
        // Check server gets replaced
        serverConfigManager.configure(serverConfig);

        this.assertServerConfigMatches(serverConfig);
    }

    private void assertServerConfigMatches(ServerConfig serverConfig) {
        final List<PhpServer> servers = PhpServersWorkspaceStateComponent.getInstance(this.getProject()).getServers()
                .stream().filter(server -> "test.ddev.site".equals(server.getName())).toList();

        Assert.assertEquals(1, servers.size());

        final PhpServer server = servers.get(0);
        Assert.assertEquals("test.ddev.site", server.getName());
        Assert.assertEquals("test.ddev.site", server.getHost());

        var mappings = server.getMappings();

        Assert.assertEquals(1, mappings.size());

        var mapping = mappings.get(0);

        Assert.assertEquals(serverConfig.localPath(), mapping.getLocalRoot());
        Assert.assertEquals(serverConfig.remotePathPath(), mapping.getRemoteRoot());
    }
}
