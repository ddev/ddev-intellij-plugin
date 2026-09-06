package de.php_perfect.intellij.ddev.dbmanager;

import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

/**
 * Ways to open a project's database. External applications are launched through DDEV's global
 * host commands ({@code ddev tableplus}, {@code ddev dbeaver}, ...), which pass the connection on.
 */
public enum DatabaseManager {
    IDE("IDE Database Tool Window", Kind.IDE, null, null),
    PHPMYADMIN("phpMyAdmin", Kind.ADD_ON, "phpmyadmin", "ddev/ddev-phpmyadmin"),
    ADMINER("Adminer", Kind.ADD_ON, "adminer", "ddev/ddev-adminer"),
    TABLEPLUS("TablePlus", Kind.HOST_COMMAND, "tableplus", null),
    TABLEPRO("TablePro", Kind.HOST_COMMAND, "tablepro", null),
    SEQUEL_ACE("Sequel Ace", Kind.HOST_COMMAND, "sequelace", null),
    QUERIOUS("Querious", Kind.HOST_COMMAND, "querious", null),
    DBEAVER("DBeaver", Kind.HOST_COMMAND, "dbeaver", null),
    HEIDISQL("HeidiSQL", Kind.HOST_COMMAND, "heidisql", null);

    public enum Kind {
        IDE,
        ADD_ON,
        HOST_COMMAND
    }

    private final @NotNull String label;
    private final @NotNull Kind kind;
    private final @Nullable String command;
    private final @Nullable String addOn;

    DatabaseManager(@NotNull String label, @NotNull Kind kind, @Nullable String command, @Nullable String addOn) {
        this.label = label;
        this.kind = kind;
        this.command = command;
        this.addOn = addOn;
    }

    public @NotNull String label() {
        return this.label;
    }

    public @NotNull Kind kind() {
        return this.kind;
    }

    /**
     * The DDEV host command for {@link Kind#HOST_COMMAND}, or the service name for {@link Kind#ADD_ON}.
     */
    public @Nullable String command() {
        return this.command;
    }

    public @Nullable String addOn() {
        return this.addOn;
    }

    @Override
    public @NotNull String toString() {
        return this.label;
    }

    public boolean isSupported() {
        return switch (this) {
            case TABLEPRO, SEQUEL_ACE, QUERIOUS -> SystemInfo.isMac;
            case TABLEPLUS -> SystemInfo.isMac || SystemInfo.isWindows;
            case HEIDISQL -> SystemInfo.isWindows || SystemInfo.isLinux;
            default -> true;
        };
    }

    public static @NotNull List<DatabaseManager> available() {
        return Arrays.stream(values()).filter(DatabaseManager::isSupported).toList();
    }

    /**
     * Returns the manager stored under {@code value}, or {@code null} when none is configured.
     */
    public static @Nullable DatabaseManager fromValue(@Nullable String value) {
        return Arrays.stream(values()).filter(manager -> manager.label.equals(value)).findFirst().orElse(null);
    }
}
