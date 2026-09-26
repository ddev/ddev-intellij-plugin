package de.php_perfect.intellij.ddev;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public final class DdevIntegrationBundle extends DynamicBundle {

    @NonNls
    private static final @NotNull String BUNDLE = "messages.DdevIntegrationBundle";
    private static final @NotNull DdevIntegrationBundle INSTANCE = new DdevIntegrationBundle();

    private DdevIntegrationBundle() {
        super(DdevIntegrationBundle.class, BUNDLE);
    }

    @NotNull
    public static @Nls String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<@Nls String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

    public static String getName() {
        return BUNDLE;
    }
}
