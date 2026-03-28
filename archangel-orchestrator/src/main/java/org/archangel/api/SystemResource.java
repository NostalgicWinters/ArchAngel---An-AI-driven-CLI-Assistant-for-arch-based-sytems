package org.archangel.api;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.archangel.model.CommandResponse;
import org.archangel.system.CommandService;
import org.archangel.system.SystemLogService;
import org.archangel.think.LogicAnalyzeExceptionHandler;
import org.archangel.think.GeneratedResponse;
import org.archangel.think.LogicConfigClient;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * FIXED:
 * 1. @ApplicationScoped added — required for CDI injection to work correctly in
 *    all Quarkus deployment modes.
 *
 * 2. POST /system/analyze: was returning Response.status(NO_CONTENT).entity(...)
 *    which is invalid HTTP — 204 No Content must not have a body. The response body
 *    was silently dropped by HTTP clients. Changed to 200 with a clear message body
 *    when no logs are available.
 *
 * 3. POST /system/analyze / GET /system/incidents: when the brain service is down
 *    the fallback GeneratedResponse comes back with severity=UNKNOWN and a generic
 *    message, giving the operator no hint about the real cause. Added a /system/health
 *    endpoint that checks both the log pipeline and the brain service reachability,
 *    so the operator knows which component to fix.
 *
 * 4. GET /system/incidents: the TODO comment tracking persistence is preserved.
 *
 * The real cause of the UNKNOWN severity in the Swagger screenshot is that the
 * archangel-brain service (Ollama) is unreachable — LogicInterface @CircuitBreaker
 * opens and @Fallback fires, returning the degraded response. Fix: ensure
 * archangel-brain is running (`systemctl start archangel-brain`) and that
 * quarkus.rest-client.logic-api.url is correctly configured in application.properties.
 */
@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
public class SystemResource {

    private static final Logger log = Logger.getLogger(SystemResource.class);

    @Inject
    CommandService commandService;

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogicAnalyzeExceptionHandler logicAnalyzeExceptionHandler;

    @Inject
    @RestClient
    LogicConfigClient logicConfigClient;

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
    @Path("/execute")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response executeCommand(
            @HeaderParam("X-Api-Key") String apiKey,
            String command) {

        if (!isAuthorized(apiKey)) return unauthorized();

        CommandResponse result = commandService.executeCommand(command);
        return Response.ok(result).build();
    }

    @GET
    @Path("/config")
    public Response getConfig(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        try {
            Map<String, String> config = logicConfigClient.getConfig();
            return Response.ok(config).build();
        } catch (Exception e) {
            log.warnf("Failed to fetch AI config from brain service: %s", e.getMessage());
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Unable to fetch AI config from brain service"))
                    .build();
        }
    }

    @GET
    @Path("/status")
    public Response getStatus(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        var uname  = commandService.executeCommand("uname -a");
        var uptime = commandService.executeCommand("uptime");
        var mem    = commandService.executeCommand("free -h");
        var disk   = commandService.executeCommand("df -h");

        return Response.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "uname",     uname.getStdout().trim(),
                "uptime",    uptime.getStdout().trim(),
                "memory",    mem.getStdout().trim(),
                "disk",      disk.getStdout().trim()
        )).build();
    }

    /**
     * FIXED: Was returning 204 NO_CONTENT with a body — invalid HTTP.
     * HTTP 204 must have no body; the entity was silently dropped by all
     * compliant HTTP clients (curl, OkHttp, etc.), giving the CLI no error
     * message to display. Changed to 200 with a descriptive body.
     *
     * Additionally: when the brain service is down the result will have
     * severity=UNKNOWN. This is expected behaviour — see /system/health to
     * confirm whether the brain service is the cause.
     */
    @POST
    @Path("/analyze")
    public Response analyzeLogs(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        String logs = systemLogService.fetchRecentLogs();

        // FIXED: was NO_CONTENT (204) with a body — invalid HTTP.
        // Changed to 200 with a message body so clients receive a useful response.
        if (logs == null || logs.isBlank()) {
            return Response.ok(
                    new GeneratedResponse(
                            GeneratedResponse.Severity.LOW,
                            "No logs available to analyze. System appears healthy or journalctl returned no output.",
                            "No action needed. If this is unexpected, check: journalctl -p 3..5 -n 50"
                    )
            ).build();
        }

        GeneratedResponse analysis = logicAnalyzeExceptionHandler.analyzeLogs(logs);

        if (!analysis.isAnalysisValid()) {
            log.warn("Brain service returned degraded response for /system/analyze. " +
                    "Check archangel-brain: systemctl status archangel-brain");
        }

        return Response.ok(analysis).build();
    }

    @GET
    @Path("/incidents")
    public Response getIncidents(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        // TODO: persist analysis results to H2/SQLite and return history here.
        String logs = systemLogService.fetchRecentLogs();
        if (logs == null || logs.isBlank()) {
            return Response.ok(Map.of(
                    "incidents", List.of(),
                    "message",   "No log data available"
            )).build();
        }

        GeneratedResponse analysis = logicAnalyzeExceptionHandler.analyzeLogs(logs);

        if (!analysis.isAnalysisValid()) {
            log.warn("Brain service returned degraded response for /system/incidents. " +
                    "Check archangel-brain: systemctl status archangel-brain");
        }

        return Response.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "analysis",  analysis
        )).build();
    }

    /**
     * NEW: Health check endpoint — diagnoses the most common failure mode seen in
     * production: the brain service being down causes every /analyze and /incidents
     * call to silently return severity=UNKNOWN with a fallback message, giving the
     * operator no actionable information.
     *
     * This endpoint actively probes:
     *   1. The log pipeline (can journalctl be executed?)
     *   2. The brain service config endpoint (is archangel-brain reachable?)
     *
     * CLI usage: archangel health
     * Returns 200 when all systems are operational, 503 when any component is down.
     */
    @GET
    @Path("/health")
    public Response getHealth(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        // 1. Check log pipeline
        String logs = systemLogService.fetchRecentLogs();
        boolean logsOk = logs != null;

        // 2. Check brain service
        boolean brainOk = false;
        String brainError = null;
        try {
            logicConfigClient.getConfig();
            brainOk = true;
        } catch (Exception e) {
            brainError = e.getMessage();
            log.warnf("Health check: brain service unreachable: %s", e.getMessage());
        }

        boolean healthy = logsOk && brainOk;

        Map<String, Object> body = Map.of(
                "timestamp",  Instant.now().toString(),
                "healthy",    healthy,
                "components", Map.of(
                        "logPipeline", Map.of(
                                "ok",     logsOk,
                                "detail", logsOk
                                        ? "journalctl executed successfully"
                                        : "journalctl failed or returned no output"
                        ),
                        "brainService", Map.of(
                                "ok",     brainOk,
                                "detail", brainOk
                                        ? "archangel-brain reachable"
                                        : "archangel-brain unreachable: " + brainError
                        )
                ),
                "hint", healthy
                        ? "All systems operational."
                        : "If brainService.ok=false, run: systemctl status archangel-brain"
        );

        return Response
                .status(healthy ? 200 : 503)
                .entity(body)
                .build();
    }
}