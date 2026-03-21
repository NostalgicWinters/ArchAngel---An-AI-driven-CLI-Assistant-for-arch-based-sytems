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

import java.time.temporal.ChronoUnit;

/**
 * REST client interface for the Python brain service.
 *
 * FIXED:
 * 1. Parameter changed from raw String → AnalyzeRequest (fixes broken wire contract)
 * 2. @Timeout: prevents 5s+ hangs blocking Quarkus worker threads when Ollama is slow
 * 3. @CircuitBreaker: after 3 failures in a 10s window, open the circuit for 30s
 *    so Java stops hammering a down AI service on every request
 * 4. @Fallback: delegates to LogicAnalyzeExceptionHandler when circuit is open or timeout fires
 *    — Java always returns a valid GeneratedResponse shape, never propagates a 500 to the CLI
 */
@Path("/analyze")
@RegisterRestClient(configKey = "logic-api")
public interface LogicInterface {

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
    @Fallback(fallbackMethod = "fallbackAnalyze",
            applyOn = {Exception.class})
    GeneratedResponse analyze(AnalyzeRequest request);

    /**
     * Fallback invoked when circuit is open, timeout fires, or any exception escapes.
     * Must have identical signature to the primary method.
     */
    default GeneratedResponse fallbackAnalyze(AnalyzeRequest request) {
        GeneratedResponse r = new GeneratedResponse();
        r.setSeverity(GeneratedResponse.Severity.UNKNOWN);
        r.setSummary("AI analysis service is currently unavailable. Logs were collected but not analyzed.");
        r.setRecommendedAction("Check brain service: systemctl status archangel-brain");
        return r;
    }
}