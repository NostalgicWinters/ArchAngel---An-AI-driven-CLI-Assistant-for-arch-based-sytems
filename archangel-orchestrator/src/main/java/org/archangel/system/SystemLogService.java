package org.archangel.system;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class SystemLogService {

    private static final Logger log = Logger.getLogger(SystemLogService.class);

    @Inject
    SystemProcessExec systemProcessExec;

    // FIXED: This method was defined but never called by any scheduler or endpoint.
    // It is now invoked by SystemResource.analyzeLogs() and SystemResource.getIncidents().
    //
    // Fetches priority 3–5 (err, warning, notice) last 50 lines.
    // This is the correct command to get actionable log entries without flooding
    // the AI with debug/info noise.
    public String fetchRecentLogs() {
        ProcessResult result = systemProcessExec.execute(
                List.of("journalctl", "-p", "3..5", "-n", "50", "--no-pager", "--output=short-iso")
        );

        if (result.isTimedOut()) {
            log.error("journalctl timed out");
            return null;
        }

        if (result.getExitCode() != 0) {
            log.error("journalctl failed: " + result.getStderr());
            return null;
        }

        String output = result.getStdout();

        if (output == null || output.isBlank()) {
            return ""; // IMPORTANT: not null
        }

        return output;
    }
}