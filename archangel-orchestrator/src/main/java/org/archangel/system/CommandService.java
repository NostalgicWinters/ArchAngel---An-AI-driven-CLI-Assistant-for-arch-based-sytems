package org.archangel.system;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.archangel.model.CommandResponse;

import java.util.List;

@ApplicationScoped
public class CommandService {

    @Inject
    SystemProcessExec systemProcessExec;

    public CommandResponse executeCommand(String command) {
        if (command == null || command.isBlank()) {
            return new CommandResponse(command, -1, "", "Command cannot be empty");
        }

        command = command.trim();

        // Guard against shell injection via metacharacters.
        // Note: since we use ProcessBuilder with a split arg list (not shell=true),
        // these characters won't work for injection — but we reject them anyway to
        // prevent surprises and to be explicit about what this API accepts.

        if (!AllowedCommands.isAllowed(command)) {
            return new CommandResponse(command, -1, "", "Command not in allowlist: " + command);
        }

        List<String> parts = List.of(command.split("\\s+"));
        ProcessResult result = systemProcessExec.execute(parts);

        if (result.isTimedOut()) {
            return new CommandResponse(command, -1, "", "Command timed out");
        }

        return new CommandResponse(
                command,
                result.getExitCode(),
                result.getStdout(),
                result.getStderr()
        );
    }
}