package de.php_perfect.intellij.ddev.state;

import de.php_perfect.intellij.ddev.cmd.Description;
import de.php_perfect.intellij.ddev.version.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

final class StateImplTest {
    @Test
    void equalStatesMatch() {
        final StateImpl state = state(true, "v1.19.0", "acol", "/bin/ddev");
        final StateImpl other = state(true, "v1.19.0", "acol", "/bin/ddev");

        Assertions.assertEquals(state, state);
        Assertions.assertEquals(state, other);
        Assertions.assertEquals(state.hashCode(), other.hashCode());
    }

    @Test
    void differingStatesDoNotMatch() {
        final StateImpl state = state(true, "v1.19.0", "acol", "/bin/ddev");

        Assertions.assertNotEquals(null, state);
        Assertions.assertNotEquals(new Object(), state);
        Assertions.assertNotEquals(state(false, "v1.19.0", "acol", "/bin/ddev"), state);
        Assertions.assertNotEquals(state(true, "v1.20.0", "acol", "/bin/ddev"), state);
        Assertions.assertNotEquals(state(true, "v1.19.0", "other", "/bin/ddev"), state);
        Assertions.assertNotEquals(state(true, "v1.19.0", "acol", "/usr/bin/ddev"), state);
    }

    @Test
    void resetClearsState() {
        final StateImpl state = state(true, "v1.19.0", "acol", "/bin/ddev");

        state.reset();

        Assertions.assertEquals(new StateImpl(), state);
        Assertions.assertFalse(state.isBinaryConfigured());
        Assertions.assertFalse(state.isAvailable());
    }

    @Test
    void emptyBinaryIsNotConfigured() {
        final StateImpl state = new StateImpl();
        state.setDdevBinary("");

        Assertions.assertFalse(state.isBinaryConfigured());
    }

    @Test
    void toStringListsFields() {
        final StateImpl state = new StateImpl();
        state.setDdevBinary("/bin/ddev");
        state.setConfigured(true);

        Assertions.assertEquals("StateImpl{version=null, description=null, ddevBinary='/bin/ddev', configured=true}", state.toString());
    }

    private static StateImpl state(boolean configured, String version, String name, String binary) {
        final StateImpl state = new StateImpl();
        state.setConfigured(configured);
        state.setDdevVersion(new Version(version));
        state.setDescription(Description.builder().name(name).build());
        state.setDdevBinary(binary);
        return state;
    }
}
