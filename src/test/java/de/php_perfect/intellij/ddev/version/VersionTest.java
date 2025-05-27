package de.php_perfect.intellij.ddev.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

final class VersionTest {
    @ParameterizedTest
    @ValueSource(strings = {"1.26.6", "v1.26.6", "1.26.6-DEBUG"})
    void versionsWithPrefixAndSuffixAreParsedCorrectly(String versionString) {
        final Version version = new Version(versionString);

        Assertions.assertArrayEquals(new int[]{1, 26, 6}, version.numbers);
    }

    @Test
    void headVersionIsParsedCorrectly() {
        final Version version = new Version("v1.24.4-36-ge74e3a95f");

        Assertions.assertArrayEquals(new int[]{1, 24, 4}, version.numbers);
        Assertions.assertTrue(version.isHeadVersion());
        Assertions.assertEquals("36-ge74e3a95f", version.getBuildInfo());
        Assertions.assertEquals("v1.24.4-36-ge74e3a95f", version.toString());
    }

    @Test
    void regularVersionIsNotHeadVersion() {
        final Version version = new Version("v1.24.4");

        Assertions.assertFalse(version.isHeadVersion());
        Assertions.assertNull(version.getBuildInfo());
    }

    @Test
    void debugVersionIsNotHeadVersion() {
        final Version version = new Version("v1.24.4-DEBUG");

        Assertions.assertFalse(version.isHeadVersion());
        Assertions.assertNull(version.getBuildInfo());
    }

    @Test
    void compareTo_withEarlierVersion_isGreaterThan() {
        Assertions.assertEquals(1, new Version("2.0.0").compareTo(new Version("1.0.0")));
    }

    @Test
    void compareTo_withSameVersion_isEqual() {
        Assertions.assertEquals(0, new Version("2.0.0").compareTo(new Version("2.0.0")));
    }

    @Test
    void compareTo_withLaterVersion_isLessThan() {
        Assertions.assertEquals(-1, new Version("1.0.0").compareTo(new Version("2.0.0")));
    }

    @Test
    void compareTo_withMorePreciseSameVersion_isFalse() {
        Assertions.assertEquals(0, new Version("1").compareTo(new Version("1.0.0")));
    }

    @Test
    void compareTo_withMorePreciseEarlierVersion_isFalse() {
        Assertions.assertEquals(1, new Version("2").compareTo(new Version("1.0.0")));
    }

    @Test
    void compareTo_withMorePreciseLaterVersion_isLessThan() {
        Assertions.assertEquals(-1, new Version("1").compareTo(new Version("1.0.1")));
    }

    @Test
    void compareTo_headVersionWithSameBaseVersion_isEqual() {
        Assertions.assertEquals(0, new Version("v1.24.4").compareTo(new Version("v1.24.4-36-ge74e3a95f")));
    }

    @Test
    void compareTo_headVersionWithDifferentBaseVersion_isUnequal() {
        Assertions.assertEquals(-1, new Version("v1.24.3-42-gabcdef12").compareTo(new Version("v1.24.4")));
        Assertions.assertEquals(1, new Version("v1.24.5-42-gabcdef12").compareTo(new Version("v1.24.4")));
    }
}
