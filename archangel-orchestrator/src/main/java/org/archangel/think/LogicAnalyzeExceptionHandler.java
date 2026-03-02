package org.archangel.think;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class LogicAnalyzeExceptionHandler
{
    @Inject
    LogicInterface logic;
    public GeneratedResponse analyzeLogs(String logs) {
        try {
            return logic.analyze(logs);
        }
        catch (Exception e) {
            GeneratedResponse fallBackResponse = new GeneratedResponse();
            fallBackResponse.setSeverity("UNKNOWN");
            fallBackResponse.setSummary("Brain service unavailable: " + e.getMessage());
            fallBackResponse.setRecommendedAction(null);

            return fallBackResponse;
        }
    }

}
