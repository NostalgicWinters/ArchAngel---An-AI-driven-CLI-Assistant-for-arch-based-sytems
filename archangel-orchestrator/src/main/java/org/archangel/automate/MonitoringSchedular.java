package org.archangel.automate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import org.archangel.system.SystemLogService;
import org.archangel.system.CommandService;
import org.archangel.model.CommandResponse;

import org.archangel.think.LogicAnalyzeExceptionHandler;
import org.archangel.think.GeneratedResponse;



@ApplicationScoped
public class MonitoringSchedular
{
    private static final Logger log = Logger.getLogger(MonitoringSchedular.class);

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogicAnalyzeExceptionHandler logicAnalyze;

    @Inject
    CommandService  commandService;
    public void monitorinSystem(){
     try {
         String logs = systemLogService.fetchRecentLogs();
         if (logs == null || logs.isBlank()) {
             log.info("No logs fetched.");
             return;
         }

         GeneratedResponse generatedResponse = logicAnalyze.analyzeLogs(logs);

         if (generatedResponse == null) {
             log.warn("AI returned null response.");
             return;

         }
         log.info("AI Severity: " + generatedResponse.getSeverity());
         log.info("AI Summary: " +  generatedResponse.getSummary());

     }

}
}
