package de.php_perfect.intellij.ddev.version;

import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.io.HttpRequests;
import com.intellij.util.io.RequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.IOException;

final class GithubClientTest extends BasePlatformTestCase {
    private static final String RELEASE_URL = "https://github.com/ddev/ddev/releases/latest";

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
    void parsesLatestRelease() throws IOException {
        final ProgressIndicator indicator = new EmptyProgressIndicator();
        final RequestBuilder requestBuilder = Mockito.mock(RequestBuilder.class, Answers.RETURNS_SELF);
        Mockito.doReturn("{\"tag_name\":\"v1.23.0\",\"name\":\"v1.23.0\"}").when(requestBuilder).readString(indicator);

        try (MockedStatic<HttpRequests> httpRequests = Mockito.mockStatic(HttpRequests.class)) {
            httpRequests.when(() -> HttpRequests.request(RELEASE_URL)).thenReturn(requestBuilder);

            Assertions.assertEquals(new LatestRelease("v1.23.0"), new GithubClient().loadCurrentVersion(indicator));
        }

        Mockito.verify(requestBuilder).accept("application/json");
        Mockito.verify(requestBuilder).redirectLimit(2);
    }

    @Test
    void unreachableGithubYieldsNoRelease() throws IOException {
        final ProgressIndicator indicator = new EmptyProgressIndicator();
        final RequestBuilder requestBuilder = Mockito.mock(RequestBuilder.class, Answers.RETURNS_SELF);
        Mockito.doThrow(new IOException("offline")).when(requestBuilder).readString(indicator);

        try (MockedStatic<HttpRequests> httpRequests = Mockito.mockStatic(HttpRequests.class)) {
            httpRequests.when(() -> HttpRequests.request(RELEASE_URL)).thenReturn(requestBuilder);

            Assertions.assertNull(new GithubClient().loadCurrentVersion(indicator));
        }
    }

    @Test
    void registeredClientIsGithub() {
        Assertions.assertInstanceOf(GithubClient.class, ReleaseClient.getInstance());
    }
}
