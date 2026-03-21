package org.archangel.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.archangel.system.LogDecisionService;
import org.archangel.system.SystemLogService;
import org.archangel.think.GeneratedResponse;
import org.archangel.think.LogicAnalyzeExceptionHandler;
import org.jboss.logging.Logger;

/**
 * NEW ENDPOINT: Exposes on-demand log analysis to the CLI.
 *
 * Previously the Java orchestrator had AI analysis wired up internally
 * (LogicAnalyzeExceptionHandler) but never exposed it via any HTTP endpoint.
 * The CLI worked around this by calling Ollama directly — an architectural violation
 * that required every user to have Ollama installed locally.
 *
 * This resource:
 * 1. Triggers log collection via SystemLogService (journalctl)
 * 2. Passes logs to the Python brain service via LogicAnalyzeExceptionHandler
 * 3. Returns structured GeneratedResponse to the CLI
 *
 * The CLI summary() command should call POST /analysis/logs instead of
 * running Ollama directly.
 */
@Path("/analysis")
@Produces(MediaType.APPLICATION_JSON)
public class AnalysisResource {

    private static final Logger log = Logger.getLogger(AnalysisResource.class);

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogDecisionService logDecisionService;

    @Inject
    LogicAnalyzeExceptionHandler analyzeHandler;

    /**
     * Trigger log analysis on demand.
     * Collects recent journalctl output and sends it to the AI brain service.
     */
    @POST
    @Path("/logs")
    public Response analyzeLogs() {

        String logs = systemLogService.fetchRecentLogs();

        if (logs == null) {
            return Response.ok(
                    new GeneratedResponse(
                            GeneratedResponse.Severity.LOW,
                            "No relevant logs found. System appears healthy.",
                            "No action needed."
                    )
            ).build();
        }

        if (!logDecisionService.shouldCallAI(logs)) {
            return Response.ok(
                    new GeneratedResponse(
                            GeneratedResponse.Severity.LOW,
                            "No significant issues detected in logs.",
                            "No action required."
                    )
            ).build();
        }

        GeneratedResponse result = analyzeHandler.analyzeLogs(logs);
        return Response.ok(result).build();
    }

    /**
     * Analyze custom log text submitted by the caller.
     * Useful for the CLI's config_scan or targeted analysis.
     */
    @POST
    @Path("/custom")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response analyzeCustom(String logText) {
        if (logText == null || logText.isBlank()) {
            return Response.status(400)
                    .entity("{\"error\": \"Log text cannot be empty\"}")
                    .build();
        }

        if (logText.length() > 50_000) {
            logText = "[TRUNCATED]\n" + logText.substring(logText.length() - 50_000);
        }

        GeneratedResponse result = analyzeHandler.analyzeLogs(logText);
        return Response.ok(result).build();
    }
}