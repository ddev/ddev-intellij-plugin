package de.php_perfect.intellij.ddev.version;

import com.intellij.ide.util.PropertiesComponent;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

final class DdevUpgradeListenerTest {
    @Test
    void reportsOnlyTheFirstSightingOfANewerVersion() {
        final PropertiesComponent properties = properties();

        assertThat(DdevUpgradeListener.recordVersion(properties, new Version("v1.24.10"))).isNull();
        assertThat(DdevUpgradeListener.recordVersion(properties, new Version("v1.25.0"))).isEqualTo("v1.24.10");
        assertThat(DdevUpgradeListener.recordVersion(properties, new Version("v1.25.0"))).isNull();
    }

    @Test
    void aDowngradeIsNotReported() {
        final PropertiesComponent properties = properties();

        DdevUpgradeListener.recordVersion(properties, new Version("v1.25.0"));
        assertThat(DdevUpgradeListener.recordVersion(properties, new Version("v1.24.10"))).isNull();
    }

    private static PropertiesComponent properties() {
        final Map<String, String> values = new HashMap<>();
        final PropertiesComponent properties = mock(PropertiesComponent.class);
        when(properties.getValue(anyString())).thenAnswer(invocation -> values.get(invocation.<String>getArgument(0)));
        doAnswer(invocation -> values.put(invocation.getArgument(0), invocation.getArgument(1)))
                .when(properties).setValue(anyString(), anyString());
        return properties;
    }
}
