package de.php_perfect.intellij.ddev.index;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class ManagedConfigurationIndexTest extends BasePlatformTestCase {
    private static final String INDEX_KEY = "de.php_perfect.intellij.ddev.index";

    private static class SomeConfiguration implements IndexableConfiguration {
        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static class SomeOtherConfiguration implements IndexableConfiguration {
        @Override
        public int hashCode() {
            return 2;
        }
    }

    @Override
    @BeforeEach
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Test
    void addToIndexCheckId() {
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());
        managedConfigurationIndex.set("as73asvb324", SomeConfiguration.class, 123);

        assertTrue(managedConfigurationIndex.isManaged("as73asvb324", SomeConfiguration.class));
        assertFalse(managedConfigurationIndex.isManaged("fooBar", SomeConfiguration.class));
    }

    @Test
    void getFromIndex() {
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());
        managedConfigurationIndex.set("as73asvb324", SomeConfiguration.class, 123);

        assertSame("as73asvb324", managedConfigurationIndex.get(SomeConfiguration.class).id());
        assertSame(null, managedConfigurationIndex.get(SomeOtherConfiguration.class));
    }

    @Test
    void addToIndexCheckType() {
        final String key = "as73asvb324";
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());
        managedConfigurationIndex.set(key, SomeConfiguration.class, 123);

        assertTrue(managedConfigurationIndex.isManaged(key, SomeConfiguration.class));
        assertFalse(managedConfigurationIndex.isManaged(key, SomeOtherConfiguration.class));
    }

    @Test
    void removeIndex() {
        final String key = "as73asvb324";
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());

        managedConfigurationIndex.set(key, SomeConfiguration.class, 123);
        assertTrue(managedConfigurationIndex.isManaged(key, SomeConfiguration.class));

        managedConfigurationIndex.remove(SomeConfiguration.class);
        assertFalse(managedConfigurationIndex.isManaged(key, SomeConfiguration.class));
    }

    @Test
    void purgeIndex() {
        final String key = "as73asvb324";
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());

        managedConfigurationIndex.set(key, SomeConfiguration.class, 123);
        assertTrue(managedConfigurationIndex.isManaged(key, SomeConfiguration.class));

        managedConfigurationIndex.purge();
        assertFalse(managedConfigurationIndex.isManaged(key, SomeConfiguration.class));
    }

    @Test
    void isUpToDate() {
        final String key = "as73asvb324";
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());

        managedConfigurationIndex.set(key, SomeConfiguration.class, 123);
        assertTrue(managedConfigurationIndex.isUpToDate(SomeConfiguration.class, 123));
        assertFalse(managedConfigurationIndex.isUpToDate(SomeConfiguration.class, 321));
    }

    @Test
    void isUpToDateNonExistent() {
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());

        assertFalse(managedConfigurationIndex.isUpToDate(SomeConfiguration.class, 123));
    }

    @Test
    void repeatedSetKeepsOneIndexEntry() {
        final ManagedConfigurationIndex managedConfigurationIndex = ManagedConfigurationIndex.getInstance(this.getProject());

        managedConfigurationIndex.set("as73asvb324", SomeConfiguration.class, 123);
        managedConfigurationIndex.set("as73asvb324", SomeConfiguration.class, 321);
        managedConfigurationIndex.set("bc84btwc435", SomeOtherConfiguration.class, 123);

        assertEquals(List.of(keyOf(SomeConfiguration.class), keyOf(SomeOtherConfiguration.class)), this.indexList());
    }

    @Test
    void setCollapsesDuplicateIndexEntries() {
        final String key = keyOf(SomeConfiguration.class);
        PropertiesComponent.getInstance(this.getProject()).setList(INDEX_KEY, List.of(key, key, key));

        ManagedConfigurationIndex.getInstance(this.getProject()).set("as73asvb324", SomeConfiguration.class, 123);

        assertEquals(List.of(key), this.indexList());
    }

    @Test
    void removeDropsDuplicateIndexEntries() {
        final String key = keyOf(SomeConfiguration.class);
        final String otherKey = keyOf(SomeOtherConfiguration.class);
        PropertiesComponent.getInstance(this.getProject()).setList(INDEX_KEY, List.of(key, otherKey, key));

        ManagedConfigurationIndex.getInstance(this.getProject()).remove(SomeConfiguration.class);

        assertEquals(List.of(otherKey), this.indexList());
    }

    private List<String> indexList() {
        return PropertiesComponent.getInstance(this.getProject()).getList(INDEX_KEY);
    }

    private static String keyOf(final Class<? extends IndexableConfiguration> type) {
        return "de.php_perfect.intellij.ddev." + type.getName();
    }

    @Override
    @AfterEach
    protected void tearDown() throws Exception {
        ManagedConfigurationIndex.getInstance(this.getProject()).purge();

        super.tearDown();
    }
}
