package de.php_perfect.intellij.ddev.util;

import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import de.php_perfect.intellij.ddev.notification.DdevNotifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

final class PluginCheckerTest extends BasePlatformTestCase {
    private static final String INSTALLED_PLUGIN = "de.php_perfect.intellij.ddev";

    private DdevNotifier notifier;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.notifier = Mockito.mock(DdevNotifier.class);
        ServiceContainerUtil.replaceService(getProject(), DdevNotifier.class, this.notifier, getTestRootDisposable());
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    void installedPluginIsNotMissing() {
        Assertions.assertEquals(List.of(), PluginChecker.getMissingPlugins(List.of(INSTALLED_PLUGIN)));
    }

    @Test
    void missingPluginsAreListedByDisplayName() {
        final List<String> missing = PluginChecker.getMissingPlugins(List.of("com.example.missing", INSTALLED_PLUGIN, "org.jetbrains.plugins.missing-terminal"));

        Assertions.assertEquals(List.of("com.example.missing", "org.jetbrains.plugins.missing-terminal"), missing);
    }

    @Test
    void noNotificationWhenAllPluginsArePresent() {
        Assertions.assertFalse(PluginChecker.isMissingRequiredPlugins(getProject(), List.of(INSTALLED_PLUGIN), "Database"));

        Mockito.verifyNoInteractions(this.notifier);
    }

    @Test
    void singleMissingPluginIsNotified() {
        Assertions.assertTrue(PluginChecker.isMissingRequiredPlugins(getProject(), List.of(INSTALLED_PLUGIN, "com.example.missing"), "Database"));

        Mockito.verify(this.notifier).notifyMissingPlugin("com.example.missing", "Database");
        Mockito.verifyNoMoreInteractions(this.notifier);
    }

    @Test
    void multipleMissingPluginsAreNotifiedTogether() {
        Assertions.assertTrue(PluginChecker.isMissingRequiredPlugins(getProject(), List.of("com.example.first", "com.example.second"), "PHP interpreter"));

        Mockito.verify(this.notifier).notifyMissingPlugins("com.example.first, com.example.second", "PHP interpreter");
        Mockito.verifyNoMoreInteractions(this.notifier);
    }
}
