package org.archangel.think;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

/**
 * Thin orchestration layer between Java's log collection and the Python brain service.
 *
 * FIXED:
 * 1. Uses AnalyzeRequest.fromJournal() — correct wire format to Python
 * 2. Null recommendedAction removed — fallback now always returns a non-null string
 * 3. Exception message no longer leaked to callers — internal detail logged only
 * 4. @RestClient injection qualifier added (required by MicroProfile REST Client)
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
                log.error("AI service call failed", e);
                return degradedResponse("AI service unavailable.");
            }

            validateResponse(response);
            if (!response.isAnalysisValid()) {
                log.warn("AI returned degraded or incomplete response");
            }

            return response;

        } catch (Exception e) {
            log.errorf("Unexpected error during log analysis: %s", e.getMessage());
            return degradedResponse("Unexpected error during analysis. See orchestrator logs.");
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
        r.setRecommendedAction("Check archangel-brain service status.");
        return r;
    }
}