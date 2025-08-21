package de.php_perfect.intellij.ddev.php.composer;

import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * Process handler for DDEV Composer commands.
 */
public class DdevComposerProcessHandler extends OSProcessHandler {
    
    public DdevComposerProcessHandler(@NotNull Process process, @NotNull String commandLine) {
        super(process, commandLine, Charset.defaultCharset());
        ProcessTerminatedListener.attach(this);
    }
}
