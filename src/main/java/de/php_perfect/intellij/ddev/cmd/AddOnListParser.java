package de.php_perfect.intellij.ddev.cmd;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/** Parses the human-readable add-on table embedded in recent DDEV JSON log output. */
final class AddOnListParser {
    private AddOnListParser() {
    }

    static @NotNull List<AddOn> parse(@NotNull String output) {
        final ArrayList<Row> rows = new ArrayList<>();

        for (String logLine : output.lines().toList()) {
            final String message;
            try {
                final JsonObject object = com.google.gson.JsonParser.parseString(logLine).getAsJsonObject();
                message = object.has("msg") ? object.get("msg").getAsString() : "";
            } catch (RuntimeException ignored) {
                continue;
            }

            for (String tableLine : message.lines().toList()) {
                final String trimmed = tableLine.trim();
                if (!trimmed.startsWith("│") || !trimmed.endsWith("│")) {
                    continue;
                }

                final String[] cells = trimmed.split("│", -1);
                if (cells.length < 4) {
                    continue;
                }

                final String title = cells[1].trim();
                final String description = cells[2].trim();
                if ("ADD-ON".equals(title)) {
                    continue;
                }

                if (title.isEmpty()) {
                    if (!description.isEmpty() && !rows.isEmpty()) {
                        rows.getLast().appendDescription(description);
                    }
                } else {
                    rows.add(new Row(title, description));
                }
            }
        }

        return rows.stream().map(row -> new AddOn(row.title, row.description,
                row.title.startsWith("ddev/") ? "official" : "contrib", null)).toList();
    }

    private static final class Row {
        private final @NotNull String title;
        private @NotNull String description;

        private Row(@NotNull String title, @NotNull String description) {
            this.title = title;
            this.description = description;
        }

        private void appendDescription(@NotNull String continuation) {
            this.description = this.description.isEmpty() ? continuation : this.description + " " + continuation;
        }
    }
}
