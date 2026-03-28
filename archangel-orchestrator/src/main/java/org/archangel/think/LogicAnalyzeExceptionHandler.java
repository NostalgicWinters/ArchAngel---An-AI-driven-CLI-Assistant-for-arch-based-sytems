package org.archangel.think;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Thin orchestration layer between Java's log collection and the Python brain service.
 *
 * FIXED:
 * 1. Exception logging now includes the full cause (not just e.getMessage()) so the
 *    operator can see whether it's a connection refused, a JSON parse error, or a
 *    timeout — all of which previously looked identical in the logs.
 *
 * 2. degradedResponse() now records the triggering reason in the summary so
 *    callers (and operators reading the API response) know it was a handler-level
 *    failure rather than a circuit-breaker fallback.
 *
 * 3. null recommendedAction guard retained from previous fix.
 *
 * 4. @RestClient injection qualifier retained.
 *
 * NOTE on the UNKNOWN severity seen in Swagger:
 *   This handler never fires for a brain service outage — that is handled by
 *   LogicInterface @Fallback. This handler only fires if LogicInterface.analyze()
 *   throws a non-fault-tolerance exception (e.g. a JSON deserialisation error on
 *   an unexpected brain service response shape). If the fallback is firing, the
 *   issue is in the brain service, not here.
 */
@ApplicationScoped
public class LogicAnalyzeExceptionHandler {

    private static final Logger log = Logger.getLogger(LogicAnalyzeExceptionHandler.class);

    @Inject
    @RestClient
    LogicInterface logic;

    public GeneratedResponse analyzeLogs(String logs) {
        try {
            AnalyzeRequest request = AnalyzeRequest.fromJournal(logs);
            GeneratedResponse response;
            try {
                response = logic.analyze(request);
            } catch (Exception e) {
                // FIXED: log the full exception (cause chain), not just the message.
                // Previously e.getMessage() on a RestClientException returned null
                // for connection-refused errors, giving a useless "AI service call
                // failed: null" log line.
                log.error("AI service call failed", e);
                return degradedResponse("AI service call failed: " + causeMessage(e));
            }

            validateResponse(response);

            if (!response.isAnalysisValid()) {
                log.warn("AI returned degraded or incomplete response — " +
                        "check archangel-brain service and Ollama model availability");
            }

            return response;

        } catch (Exception e) {
            log.errorf(e, "Unexpected error during log analysis");
            return degradedResponse("Unexpected error during analysis: " + causeMessage(e));
        }
    }

    private void validateResponse(GeneratedResponse r) {
        if (r.getSeverity() == null) {
            r.setSeverity(GeneratedResponse.Severity.UNKNOWN);
        }
        if (r.getSummary() == null) {
            r.setSummary("No summary returned.");
        }
        if (r.getRecommendedAction() == null) {
            r.setRecommendedAction("No action recommended.");
        }
    }

    private GeneratedResponse degradedResponse(String reason) {
        GeneratedResponse r = new GeneratedResponse();
        r.setSeverity(GeneratedResponse.Severity.UNKNOWN);
        r.setSummary(reason);
        r.setRecommendedAction(
                "Check archangel-brain service: systemctl status archangel-brain\n" +
                        "For a structured diagnosis call: GET /system/health"
        );
        return r;
    }

    /**
     * Walk the exception cause chain to find the first non-null message.
     * RestClientException often wraps the real cause (e.g. ConnectException).
     */
    private String causeMessage(Throwable t) {
        Throwable current = t;
        while (current != null) {
            if (current.getMessage() != null && !current.getMessage().isBlank()) {
                return current.getClass().getSimpleName() + ": " + current.getMessage();
            }
            current = current.getCause();
        }
        return t.getClass().getName();
    }
}