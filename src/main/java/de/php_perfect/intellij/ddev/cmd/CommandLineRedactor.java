package de.php_perfect.intellij.ddev.cmd;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Produces diagnostic command lines without exposing credentials. */
public final class CommandLineRedactor {
    private static final String REDACTED = "<redacted>";
    private static final Set<String> SENSITIVE_OPTIONS = Set.of(
            "--password", "--pass", "--dbpass", "--db-pass", "--db-password",
            "--admin-password", "--admin_password", "--adminpass", "--admin-user-password",
            "--account-pass", "--auth", "--token", "--api-token", "--api-key"
    );
    private static final Pattern URL_CREDENTIALS = Pattern.compile("(?i)(https?://[^:/@\\s]+:)([^@/\\s]+)(@)");

    private CommandLineRedactor() {
    }

    public static @NotNull String describe(@NotNull GeneralCommandLine commandLine) {
        final List<String> parts = new ArrayList<>();
        parts.add(commandLine.getExePath());
        final List<String> parameters = commandLine.getParametersList().getList();
        boolean redactNext = false;
        for (String parameter : parameters) {
            if (redactNext) {
                parts.add(REDACTED);
                redactNext = false;
                continue;
            }
            final int equals = parameter.indexOf('=');
            final String option = (equals < 0 ? parameter : parameter.substring(0, equals))
                    .toLowerCase(Locale.ROOT);
            if (SENSITIVE_OPTIONS.contains(option)) {
                if (equals < 0) {
                    parts.add(parameter);
                    redactNext = true;
                } else {
                    parts.add(parameter.substring(0, equals + 1) + REDACTED);
                }
            } else {
                parts.add(URL_CREDENTIALS.matcher(parameter).replaceAll("$1" + REDACTED + "$3"));
            }
        }
        return ParametersListUtil.join(parts);
    }
}
