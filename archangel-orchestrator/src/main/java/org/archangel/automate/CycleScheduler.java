package org.archangel.automate;

import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class CycleScheduler {

    private static final Logger log = Logger.getLogger(CycleScheduler.class);

    @Inject
    CompleteCycle completeCycle;

    @Scheduled(every = "30s", delayed = "10s")
    public void monitorSystem() {

        try {
            log.info("Running system monitoring cycle...");
            completeCycle.monitorinSystem();
        }
        catch (Exception e) {
            log.error("Scheduler failed: " + e.getMessage());
        }
    }
}