package de.php_perfect.intellij.ddev.settings;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.util.ui.UIUtil;
import de.php_perfect.intellij.ddev.util.FeatureRequiredPlugins;
import de.php_perfect.intellij.ddev.util.PluginChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import javax.swing.*;
import java.util.List;

final class DdevSettingsComponentTest extends BasePlatformTestCase {
    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void installedPluginsShowNoWarnings() {
        try (MockedStatic<PluginChecker> pluginChecker = Mockito.mockStatic(PluginChecker.class)) {
            pluginChecker.when(() -> PluginChecker.getMissingPlugins(Mockito.anyList())).thenReturn(List.of());

            final DdevSettingsComponent component = new DdevSettingsComponent(this.getProject());

            Assertions.assertTrue(this.warningTexts(component).isEmpty());
        }
    }

    @Test
    void missingPluginsAreListedBelowTheirFeature() {
        try (MockedStatic<PluginChecker> pluginChecker = Mockito.mockStatic(PluginChecker.class)) {
            pluginChecker.when(() -> PluginChecker.getMissingPlugins(Mockito.anyList())).thenReturn(List.of());
            pluginChecker.when(() -> PluginChecker.getMissingPlugins(FeatureRequiredPlugins.DATABASE))
                    .thenReturn(List.of("Database Tools"));
            pluginChecker.when(() -> PluginChecker.getMissingPlugins(FeatureRequiredPlugins.NODE_INTERPRETER))
                    .thenReturn(List.of("Node.js", "Node.js Remote Interpreter"));

            final DdevSettingsComponent component = new DdevSettingsComponent(this.getProject());

            Assertions.assertEquals(
                    List.of(" Missing plugins: Database Tools", " Missing plugins: Node.js, Node.js Remote Interpreter"),
                    this.warningTexts(component)
            );
        }
    }

    @Test
    void settersAndGettersMirrorFormFields() {
        final DdevSettingsComponent component = new DdevSettingsComponent(this.getProject());

        component.setDdevBinary("/bin/ddev");
        component.setCheckForUpdatesStatus(true);
        component.setWatchDdevCheckboxStatus(false);
        component.setAutoConfigureDataSource(true);
        component.setAutoConfigurePhpInterpreter(false);
        component.setAutoConfigureNodeJsInterpreter(true);

        Assertions.assertEquals("/bin/ddev", component.getDdevBinary());
        Assertions.assertTrue(component.getCheckForUpdatedStatus());
        Assertions.assertFalse(component.getWatchDdevCheckboxStatus());
        Assertions.assertTrue(component.getAutoConfigureDataSource());
        Assertions.assertFalse(component.getAutoConfigurePhpInterpreter());
        Assertions.assertTrue(component.getAutoConfigureNodeJsInterpreter());
    }

    private List<String> warningTexts(DdevSettingsComponent component) {
        return UIUtil.findComponentsOfType(component.getPanel(), JLabel.class).stream()
                .map(JLabel::getText)
                .filter(text -> text != null && text.startsWith(" Missing plugins:"))
                .toList();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
