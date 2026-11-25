package de.php_perfect.intellij.ddev.index;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ManagedConfigurationIndexImpl implements ManagedConfigurationIndex {
    private static final @NotNull String NAMESPACE = "de.php_perfect.intellij.ddev.";
    private static final @NotNull String INDEX_KEY = NAMESPACE + "index";
    private static final @NotNull String HASH_SUFFIX = ".hash";
    private static final @NotNull Logger LOG = Logger.getInstance(ManagedConfigurationIndexImpl.class);
    private final @NotNull Project project;

    public ManagedConfigurationIndexImpl(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void set(@NonNls @NotNull String id, @NotNull Class<? extends IndexableConfiguration> type, int hash) {
        this.setManagedConfiguration(id, type.getName(), Integer.toHexString(hash));
    }

    private void setManagedConfiguration(@NonNls @NotNull String id, @NonNls @NotNull String type, @NonNls @NotNull String hash) {
        final String key = buildKey(type);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);

        propertiesComponent.setValue(key, id);
        propertiesComponent.setValue(buildHashKey(key), hash);

        final List<String> list = ObjectUtils.firstNonNull(propertiesComponent.getList(INDEX_KEY), List.of());
        final Set<String> keys = new LinkedHashSet<>(list);
        keys.add(key);
        propertiesComponent.setList(INDEX_KEY, new ArrayList<>(keys));

        LOG.debug(String.format("Set managed configuration id '%s' for %s", id, type));
    }

    @Override
    public @Nullable IndexEntry get(@NotNull Class<? extends IndexableConfiguration> type) {
        return this.getManagedConfiguration(type.getName());
    }

    private @Nullable IndexEntry getManagedConfiguration(@NonNls @NotNull String type) {
        final String key = buildKey(type);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);

        final String id = propertiesComponent.getValue(key);

        if(id == null){
            return null;
        }

        final String hash = propertiesComponent.getValue(buildHashKey(key));

        return new IndexEntry(id, hash);
    }

    @Override
    public boolean isManaged(@NonNls @NotNull String id, @NotNull Class<? extends IndexableConfiguration> type) {
        return this.isManagedConfiguration(id, type.getName());
    }

    private boolean isManagedConfiguration(@NonNls @NotNull String id, @NonNls @NotNull String type) {
        final String key = buildKey(type);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);

        if (!propertiesComponent.isValueSet(key)) {
            return false;
        }

        return id.equals(propertiesComponent.getValue(key));
    }

    @Override
    public boolean isUpToDate(@NotNull Class<? extends IndexableConfiguration> type, int hash) {
        return this.isUpToDate(type.getName(), Integer.toHexString(hash));
    }

    private boolean isUpToDate(@NonNls @NotNull String type, @NonNls @NotNull String hash) {
        final String key = buildKey(type);
        final String hashKey = buildHashKey(key);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);

        return propertiesComponent.isValueSet(key) &&
                propertiesComponent.isValueSet(hashKey) &&
                propertiesComponent.getValue(hashKey, "").equals(hash);
    }

    @Override
    public void remove(@NotNull Class<? extends IndexableConfiguration> type) {
        this.removeManagedConfiguration(type.getName());
    }

    private void removeManagedConfiguration(@NonNls @NotNull String type) {
        final String key = buildKey(type);
        final String hashKey = buildHashKey(key);
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);

        propertiesComponent.unsetValue(key);
        propertiesComponent.unsetValue(hashKey);

        final List<String> list = ObjectUtils.firstNonNull(propertiesComponent.getList(INDEX_KEY), List.of());
        final Set<String> keys = new LinkedHashSet<>(list);
        keys.remove(key);
        propertiesComponent.setList(INDEX_KEY, new ArrayList<>(keys));

        LOG.debug(String.format("Removed managed configuration id for %s", type));
    }

    @Override
    public void purge() {
        final PropertiesComponent propertiesComponent = PropertiesComponent.getInstance(this.project);
        final List<String> list = ObjectUtils.firstNonNull(propertiesComponent.getList(INDEX_KEY), List.of());

        for (String key : list) {
            final String hashKey = buildHashKey(key);
            propertiesComponent.unsetValue(key);
            propertiesComponent.unsetValue(hashKey);
        }

        propertiesComponent.setList(INDEX_KEY, List.of());

        LOG.debug("Configuration index purged");
    }

    @NotNull
    private static String buildKey(@NonNls @NotNull String type) {
        return NAMESPACE + type;
    }

    @NotNull
    private static String buildHashKey(@NonNls @NotNull String key) {
        return key + HASH_SUFFIX;
    }
}
