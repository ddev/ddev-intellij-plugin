package de.php_perfect.intellij.ddev.icons;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class DdevIntegrationIconsTest extends BasePlatformTestCase {
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
    void logoIconsAreLoaded() {
        Assertions.assertNotNull(DdevIntegrationIcons.DdevLogoColor);
        Assertions.assertNotNull(DdevIntegrationIcons.DdevLogoMono);
        Assertions.assertNotSame(DdevIntegrationIcons.DdevLogoColor, DdevIntegrationIcons.DdevLogoMono);
    }
}
