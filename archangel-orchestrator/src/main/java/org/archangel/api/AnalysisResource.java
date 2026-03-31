package org.archangel.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.archangel.system.LogDecisionService;
import org.archangel.system.SystemLogService;
import org.archangel.think.GeneratedResponse;
import org.archangel.think.LogicAnalyzeExceptionHandler;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.util.Map;

@Path("/analysis")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class AnalysisResource {

    private static final Logger log = Logger.getLogger(AnalysisResource.class);

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogDecisionService logDecisionService;

    @Inject
    LogicAnalyzeExceptionHandler analyzeHandler;

    @ConfigProperty(name = "archangel.api.key")
    String expectedApiKey;

    private Response unauthorized() {
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(Map.of("error", "Missing or invalid X-Api-Key header"))
                .build();
    }

    private boolean isAuthorized(String apiKey) {
        return expectedApiKey != null && expectedApiKey.equals(apiKey);
    }
    @POST
    @Path("/logs")
    public Response analyzeLogs(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

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
    @POST
    @Path("/custom")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response analyzeCustom(
            @HeaderParam("X-Api-Key") String apiKey,
            String logText) {

        if (!isAuthorized(apiKey)) return unauthorized();

        if (logText == null || logText.isBlank()) {
            return Response.status(400)
                    .entity(Map.of("error", "Log text cannot be empty"))
                    .build();
        }

        if (logText.length() > 50_000) {
            logText = "[TRUNCATED]\n" + logText.substring(logText.length() - 50_000);
        }

        GeneratedResponse result = analyzeHandler.analyzeLogs(logText);
        return Response.ok(result).build();
    }
}