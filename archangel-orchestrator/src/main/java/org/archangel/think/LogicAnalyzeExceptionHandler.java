package org.archangel.think;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class LogicAnalyzeExceptionHandler
{
    @Inject
    @RestClient
    LogicInterface logic;
    public GeneratedResponse analyzeLogs(String logs) {

        try {

            LogRequest request = new LogRequest();
            request.setLogs(logs);
            request.setHostname(java.net.InetAddress.getLocalHost().getHostName());
            request.setTimestamp(System.currentTimeMillis());

            return logic.analyze(request);

        } catch (Exception e) {

            GeneratedResponse fallBackResponse = new GeneratedResponse();
            fallBackResponse.setSeverity("UNKNOWN");
            fallBackResponse.setSummary("Brain service unavailable: " + e.getMessage());

            return fallBackResponse;
        }
    }

}
