package de.php_perfect.intellij.ddev.settings;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class DdevSettingsStateTest {
    @Test
    void defaultsEnableAllFeaturesWithoutBinary() {
        final DdevSettingsState state = new DdevSettingsState();

        Assertions.assertEquals("", state.ddevBinary);
        Assertions.assertTrue(state.checkForUpdates);
        Assertions.assertTrue(state.watchDdev);
        Assertions.assertTrue(state.autoConfigureDataSource);
        Assertions.assertTrue(state.autoConfigurePhpInterpreter);
        Assertions.assertTrue(state.autoConfigureNodeJsInterpreter);
    }

    @Test
    void stateIsTheSettingsObjectItself() {
        final DdevSettingsState state = new DdevSettingsState();

        Assertions.assertSame(state, state.getState());
    }

    @Test
    void loadStateCopiesEveryField() {
        final DdevSettingsState stored = new DdevSettingsState();
        stored.ddevBinary = "/usr/bin/ddev";
        stored.checkForUpdates = false;
        stored.watchDdev = false;
        stored.autoConfigureDataSource = false;
        stored.autoConfigurePhpInterpreter = false;
        stored.autoConfigureNodeJsInterpreter = false;

        final DdevSettingsState state = new DdevSettingsState();
        state.loadState(stored);

        Assertions.assertEquals("/usr/bin/ddev", state.ddevBinary);
        Assertions.assertFalse(state.checkForUpdates);
        Assertions.assertFalse(state.watchDdev);
        Assertions.assertFalse(state.autoConfigureDataSource);
        Assertions.assertFalse(state.autoConfigurePhpInterpreter);
        Assertions.assertFalse(state.autoConfigureNodeJsInterpreter);
    }
}
