package de.php_perfect.intellij.ddev.settings;

import com.intellij.execution.process.ProcessOutput;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.testFramework.ServiceContainerUtil;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.util.ui.UIUtil;
import de.php_perfect.intellij.ddev.cmd.MockProcessExecutor;
import de.php_perfect.intellij.ddev.cmd.ProcessExecutor;
import de.php_perfect.intellij.ddev.state.DdevStateManager;
import de.php_perfect.intellij.ddev.state.StateWatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.swing.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

final class DdevSettingsConfigurableTest extends BasePlatformTestCase {
    private DdevSettingsState settings;
    private DdevSettingsState originalSettings;
    private DdevSettingsConfigurable configurable;
    private JComponent panel;
    private StateWatcher stateWatcher;
    private CountDownLatch reinitialized;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();

        this.settings = DdevSettingsState.getInstance(this.getProject());
        this.originalSettings = new DdevSettingsState();
        this.originalSettings.loadState(this.settings);

        this.stateWatcher = Mockito.mock(StateWatcher.class);
        ServiceContainerUtil.replaceService(this.getProject(), StateWatcher.class, this.stateWatcher, this.getTestRootDisposable());

        this.reinitialized = new CountDownLatch(1);
        final DdevStateManager stateManager = Mockito.mock(DdevStateManager.class);
        Mockito.doAnswer(invocation -> {
            this.reinitialized.countDown();
            return null;
        }).when(stateManager).reinitialize();
        ServiceContainerUtil.replaceService(this.getProject(), DdevStateManager.class, stateManager, this.getTestRootDisposable());

        this.configurable = new DdevSettingsConfigurable(this.getProject());
        this.panel = this.configurable.createComponent();
    }

    @Test
    void displayNameIsSettingsTitle() {
        Assertions.assertEquals("DDEV Integration", this.configurable.getDisplayName());
        Assertions.assertEquals("DDEV Integration", DdevSettingsConfigurable.getName());
    }

    @Test
    void checkForUpdatesCheckboxIsFocusedFirst() {
        Assertions.assertSame(this.checkbox("Check for DDEV updates on startup"), this.configurable.getPreferredFocusedComponent());
    }

    @Test
    void resetShowsStoredSettings() {
        this.settings.ddevBinary = "/usr/local/bin/ddev";
        this.settings.checkForUpdates = false;
        this.settings.watchDdev = true;
        this.settings.autoConfigureDataSource = false;
        this.settings.autoConfigurePhpInterpreter = true;
        this.settings.autoConfigureNodeJsInterpreter = false;

        this.configurable.reset();

        Assertions.assertEquals("/usr/local/bin/ddev", this.binaryField().getText());
        Assertions.assertFalse(this.checkbox("Check for DDEV updates").isSelected());
        Assertions.assertTrue(this.checkbox("Watch DDEV state").isSelected());
        Assertions.assertFalse(this.checkbox("Data Source").isSelected());
        Assertions.assertTrue(this.checkbox("PHP Interpreter").isSelected());
        Assertions.assertFalse(this.checkbox("NodeJs Interpreter").isSelected());
        Assertions.assertFalse(this.configurable.isModified());
    }

    @Test
    void changingAnyFieldMarksSettingsModified() {
        this.configurable.reset();

        this.binaryField().setText("/opt/ddev");
        Assertions.assertTrue(this.configurable.isModified());
        this.configurable.reset();
        Assertions.assertFalse(this.configurable.isModified());

        for (String label : new String[]{"Check for DDEV updates", "Watch DDEV state", "Data Source", "PHP Interpreter", "NodeJs Interpreter"}) {
            final JBCheckBox checkbox = this.checkbox(label);
            checkbox.setSelected(!checkbox.isSelected());
            Assertions.assertTrue(this.configurable.isModified(), label);

            this.configurable.reset();
            Assertions.assertFalse(this.configurable.isModified(), label);
        }
    }

    @Test
    void applyWithoutBinaryStoresSettingsAndRestartsStateTracking() throws Exception {
        this.configurable.reset();
        this.binaryField().setText("");
        this.checkbox("Check for DDEV updates").setSelected(false);
        this.checkbox("Watch DDEV state").setSelected(false);
        this.checkbox("Data Source").setSelected(false);
        this.checkbox("PHP Interpreter").setSelected(false);
        this.checkbox("NodeJs Interpreter").setSelected(false);

        this.configurable.apply();

        Assertions.assertEquals("", this.settings.ddevBinary);
        Assertions.assertFalse(this.settings.checkForUpdates);
        Assertions.assertFalse(this.settings.watchDdev);
        Assertions.assertFalse(this.settings.autoConfigureDataSource);
        Assertions.assertFalse(this.settings.autoConfigurePhpInterpreter);
        Assertions.assertFalse(this.settings.autoConfigureNodeJsInterpreter);
        Assertions.assertFalse(this.configurable.isModified());
        Mockito.verify(this.stateWatcher).stopWatching();
        Assertions.assertTrue(this.reinitialized.await(10, TimeUnit.SECONDS));
    }

    @Test
    void applyStoresVerifiedBinary() throws Exception {
        this.processExecutor().addProcessOutput("/opt/ddev/bin/ddev --version",
                new ProcessOutput("ddev version v1.23.0", "", 0, false, false));
        this.configurable.reset();
        this.binaryField().setText("/opt/ddev/bin/ddev");

        this.configurable.apply();

        Assertions.assertEquals("/opt/ddev/bin/ddev", this.settings.ddevBinary);
        Mockito.verify(this.stateWatcher).stopWatching();
        Assertions.assertTrue(this.reinitialized.await(10, TimeUnit.SECONDS));
    }

    @Test
    void applyRejectsBinaryWithoutVersion() {
        this.processExecutor().addProcessOutput("/opt/broken/ddev --version",
                new ProcessOutput("", "command not found", 127, false, false));
        this.configurable.reset();
        this.binaryField().setText("/opt/broken/ddev");

        final ConfigurationException exception = Assertions.assertThrows(ConfigurationException.class, this.configurable::apply);

        Assertions.assertEquals("Invalid DDEV Binary", exception.getTitle());
        Assertions.assertEquals("No version information could be retrieved from /opt/broken/ddev.", exception.getMessage());
        Assertions.assertEquals("", this.settings.ddevBinary);
        Assertions.assertTrue(this.configurable.isModified());
        Mockito.verify(this.stateWatcher, Mockito.never()).stopWatching();
    }

    @Test
    void recreatedComponentShowsFreshForm() {
        this.binaryField().setText("/unsaved/ddev");

        this.configurable.disposeUIResources();
        this.panel = this.configurable.createComponent();
        this.configurable.reset();

        Assertions.assertEquals(this.settings.ddevBinary, this.binaryField().getText());
        Assertions.assertFalse(this.configurable.isModified());
    }

    private JBCheckBox checkbox(String labelPrefix) {
        return UIUtil.findComponentsOfType(this.panel, JBCheckBox.class).stream()
                .filter(checkbox -> checkbox.getText().startsWith(labelPrefix))
                .findFirst()
                .orElseThrow();
    }

    private TextFieldWithBrowseButton binaryField() {
        return UIUtil.findComponentOfType(this.panel, TextFieldWithBrowseButton.class);
    }

    private MockProcessExecutor processExecutor() {
        return (MockProcessExecutor) ApplicationManager.getApplication().getService(ProcessExecutor.class);
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            this.configurable.disposeUIResources();
            this.settings.loadState(this.originalSettings);
        } finally {
            super.tearDown();
        }
    }
}
