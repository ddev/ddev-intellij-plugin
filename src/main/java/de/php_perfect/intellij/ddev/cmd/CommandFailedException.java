package de.php_perfect.intellij.ddev.cmd;

import org.jetbrains.annotations.Nullable;

public class CommandFailedException extends Exception {
    /**
     * The error message DDEV itself reported, suitable for showing to the user.
     */
    private final @Nullable String ddevMessage;

    public CommandFailedException(String message) {
        this(message, (String) null);
    }

    public CommandFailedException(String message, @Nullable String ddevMessage) {
        super(message);
        this.ddevMessage = ddevMessage;
    }

    public CommandFailedException(String message, Throwable cause) {
        super(message, cause);
        this.ddevMessage = null;
    }

    public @Nullable String getDdevMessage() {
        return this.ddevMessage;
    }
}
