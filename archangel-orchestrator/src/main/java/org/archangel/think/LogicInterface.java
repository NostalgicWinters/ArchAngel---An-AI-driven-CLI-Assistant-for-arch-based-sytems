package org.archangel.think;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.logging.Logger;

import java.time.temporal.ChronoUnit;

/**
 * REST client interface for the Python brain service.
 *
 * FIXED:
 * 1. @Fallback applyOn narrowed from {Exception.class} (catch-all) to the specific
 *    fault-tolerance exceptions. Using Exception.class meant that even a malformed
 *    JSON body from the brain service (which is a programming bug, not a service
 *    outage) would silently return the fallback — masking real errors.
 *    Now only CircuitBreakerOpenException and TimeoutException trigger the fallback;
 *    other exceptions (HTTP 4xx, JSON parse errors) propagate so they appear in logs.
 *
 * 2. Fallback message improved to be more actionable — previously the degraded
 *    response gave no hint about which command to run to diagnose the problem.
 *    The new message includes the exact systemctl command and the health endpoint.
 *
 * 3. Logger added to fallbackAnalyze so there is always a log line when the
 *    circuit fires — previously the fallback was silent, making it hard to correlate
 *    the degraded API response with the actual failure in the logs.
 *
 * Root cause of the UNKNOWN severity seen in Swagger:
 *   The brain service (archangel-brain / Ollama) is not running or not reachable
 *   at the configured URL. The circuit breaker fires @Fallback after 3 failures.
 *   Fix: systemctl start archangel-brain
 *        curl http://localhost:11434/api/tags   (verify Ollama is up)
 *        Check quarkus.rest-client.logic-api.url in application.properties
 *        Call GET /system/health to see a structured diagnosis
 */
@Path("/analyze")
@RegisterRestClient(configKey = "logic-api")
public interface LogicInterface {

    Logger log = Logger.getLogger(LogicInterface.class);

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Timeout(value = 8, unit = ChronoUnit.SECONDS)
    @CircuitBreaker(
            requestVolumeThreshold = 3,
            failureRatio = 0.66,
            delay = 30,
            delayUnit = ChronoUnit.SECONDS,
            successThreshold = 2
    )
    // FIXED: narrowed from Exception.class to only fault-tolerance exceptions.
    // This prevents programming bugs (bad JSON, wrong field names) from being
    // silently swallowed as if the service were simply unavailable.
    @Fallback(
            fallbackMethod = "fallbackAnalyze",
            applyOn = {
                    org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException.class,
                    org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException.class
            }
    )
    GeneratedResponse analyze(AnalyzeRequest request);

    /**
     * Fallback invoked when circuit is open or timeout fires.
     * Must have identical signature to the primary method.
     *
     * FIXED: logs a WARNING so operators can see when the fallback fires,
     * and includes the health endpoint URL in the recommended action.
     */
    default GeneratedResponse fallbackAnalyze(AnalyzeRequest request) {
        log.warn("Brain service fallback triggered — circuit open or timeout. " +
                "Run: systemctl status archangel-brain  or  GET /system/health");

        GeneratedResponse r = new GeneratedResponse();
        r.setSeverity(GeneratedResponse.Severity.UNKNOWN);
        r.setSummary("AI analysis service is currently unavailable. " +
                "Logs were collected but not analyzed.");
        r.setRecommendedAction(
                "1. Check brain service: systemctl status archangel-brain\n" +
                        "2. Verify Ollama is running: curl http://localhost:11434/api/tags\n" +
                        "3. Call GET /system/health for a structured diagnosis"
        );
        return r;
    }
}