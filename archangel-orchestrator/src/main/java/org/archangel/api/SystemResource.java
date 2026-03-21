package org.archangel.api;

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

import java.time.Instant;
import java.util.Map;

@Path("/system")
@Produces(MediaType.APPLICATION_JSON)
public class SystemResource {

    @Inject
    CommandService commandService;

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogicAnalyzeExceptionHandler logicAnalyzeExceptionHandler;

    @Inject
    @RestClient
    LogicConfigClient logicConfigClient;

    // FIXED: API key authentication on all /system endpoints.
    // Any process on localhost could previously call /system/execute freely.
    // This is a minimal shared-secret approach suitable for a local daemon.
    // The key is set in application.properties and must be passed by the CLI.
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

    // FIXED: Accept header param for auth. In production, use a ContainerRequestFilter
    // to apply this across all secured endpoints without repeating the check.
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
            return Response.status(Response.Status.SERVICE_UNAVAILABLE)
                    .entity(Map.of("error", "Unable to fetch AI config from brain service"))
                    .build();
        }
    }

    // FIXED: /status endpoint now actually exists (CLI was calling it, getting 404).
    @GET
    @Path("/status")
    public Response getStatus(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        // Run a few safe commands to collect status data
        var uname = commandService.executeCommand("uname -a");
        var uptime = commandService.executeCommand("uptime");
        var mem = commandService.executeCommand("free -h");
        var disk = commandService.executeCommand("df -h");

        return Response.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "uname", uname.getStdout().trim(),
                "uptime", uptime.getStdout().trim(),
                "memory", mem.getStdout().trim(),
                "disk", disk.getStdout().trim()
        )).build();
    }

    // FIXED: /analyze endpoint — triggers log collection + AI analysis in one call.
    // CLI can call this instead of doing ad-hoc Ollama calls directly.
    @POST
    @Path("/analyze")
    public Response analyzeLogs(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        String logs = systemLogService.fetchRecentLogs();
        if (logs == null || logs.isBlank()) {
            return Response.status(Response.Status.NO_CONTENT)
                    .entity(Map.of("error", "No logs available to analyze"))
                    .build();
        }

        GeneratedResponse analysis = logicAnalyzeExceptionHandler.analyzeLogs(logs);
        return Response.ok(analysis).build();
    }

    // FIXED: /incidents stub — returns current analysis; persistence is a future concern.
    // Previously CLI called this endpoint and always got 404.
    @GET
    @Path("/incidents")
    public Response getIncidents(@HeaderParam("X-Api-Key") String apiKey) {
        if (!isAuthorized(apiKey)) return unauthorized();

        // For now, run a fresh analysis and return it.
        // TODO: persist analysis results to H2/SQLite and return history here.
        String logs = systemLogService.fetchRecentLogs();
        if (logs == null || logs.isBlank()) {
            return Response.ok(Map.of("incidents", java.util.List.of(), "message", "No log data available")).build();
        }

        GeneratedResponse analysis = logicAnalyzeExceptionHandler.analyzeLogs(logs);
        return Response.ok(Map.of(
                "timestamp", Instant.now().toString(),
                "analysis", analysis
        )).build();
    }
}