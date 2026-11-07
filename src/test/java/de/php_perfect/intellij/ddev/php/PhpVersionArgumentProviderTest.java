package de.php_perfect.intellij.ddev.php;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.php.config.PhpLanguageLevel;
import com.jetbrains.php.config.PhpProjectConfigurationFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

final class PhpVersionArgumentProviderTest extends BasePlatformTestCase {
    private PhpLanguageLevel originalLanguageLevel;

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
        this.originalLanguageLevel = PhpProjectConfigurationFacade.getInstance(this.getProject()).getLanguageLevel();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        try {
            PhpProjectConfigurationFacade.getInstance(this.getProject()).setLanguageLevel(this.originalLanguageLevel);
        } finally {
            super.tearDown();
        }
    }

    @Test
    void passesProjectLanguageLevel() {
        PhpProjectConfigurationFacade.getInstance(this.getProject()).setLanguageLevel(PhpLanguageLevel.PHP810);

        final List<String> arguments = new PhpVersionArgumentProvider().getAdditionalArguments(this.getProject());

        Assertions.assertEquals(List.of("--php-version", "8.1"), arguments);
    }
}
