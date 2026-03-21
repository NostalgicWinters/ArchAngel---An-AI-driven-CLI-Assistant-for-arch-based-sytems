package org.archangel.automate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import org.archangel.system.SystemLogService;
import org.archangel.system.CommandService;

import org.archangel.model.CommandResponse;

import org.archangel.think.LogicAnalyzeExceptionHandler;
import org.archangel.think.GeneratedResponse;

import org.archangel.entity.Incident;
import org.archangel.entity.Incident_Repo;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.LocalDateTime;

@ApplicationScoped
public class CompleteCycle {

    private static final Logger log = Logger.getLogger(CompleteCycle.class);

    @ConfigProperty(name = "archangel.execution.enabled", defaultValue = "false")
    boolean executionEnabled;

    @Inject
    SystemLogService systemLogService;

    @Inject
    LogicAnalyzeExceptionHandler logicAnalyze;

    @Inject
    CommandService commandService;

    @Inject
    Incident_Repo incidentRepo;

    @Inject
    CommandRateLimit commandRateLimit;

    @Transactional
    public void monitorinSystem() {

        try {

            String logs = systemLogService.fetchRecentLogs();

            if (logs == null || logs.isBlank()) {
                log.info("No logs fetched.");
                return;
            }

            if(!commandRateLimit.isAllowed())
            {
                log.warn("Command rate limit exceeded. Skipping execution.");
                return;
            }

            GeneratedResponse generatedResponse = logicAnalyze.analyzeLogs(logs);

            if (generatedResponse == null) {
                log.warn("AI returned null response.");
                return;
            }

            log.info("AI Severity: " + generatedResponse.getSeverity());
            log.info("AI Summary: " + generatedResponse.getSummary());
            Incident incident = new Incident();

            incident.setTimestamp(LocalDateTime.now());
            incident.setSeverity(generatedResponse.getSeverity());
            incident.setSummary(generatedResponse.getSummary());
            incident.setRecommendedAction(generatedResponse.getRecommendedAction());

            if (generatedResponse.getRecommendedAction() != null &&
                    !generatedResponse.getRecommendedAction().isBlank()) {

                if (!executionEnabled) {

                    log.info("Execution disabled. Suggested command: "
                            + generatedResponse.getRecommendedAction());

                    incident.setExecutedCommand("NOT_EXECUTED");
                    incident.setExitCode(-1);

                } else {

                    CommandResponse commandResponse =
                            commandService.executeCommand(generatedResponse.getRecommendedAction());

                    log.info("Command executed: " + generatedResponse.getRecommendedAction());
                    log.info("Exit code: " + commandResponse.getExitCode());

                    incident.setExecutedCommand(generatedResponse.getRecommendedAction());
                    incident.setExitCode(commandResponse.getExitCode());
                    incident.setCommandOutput(commandResponse.getStdout());
                }
            }

            incidentRepo.persist(incident);

        } catch (Exception e) {
            log.error("Monitoring cycle failed: " + e.getMessage());
        }
    }
}