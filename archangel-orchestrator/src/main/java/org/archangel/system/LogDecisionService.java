package org.archangel.system;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class LogDecisionService {

    public boolean shouldCallAI(String logs) {
        if (logs == null || logs.isBlank()) return false;

        String lower = logs.toLowerCase();

        int errorCount = lower.split("error", -1).length - 1;

        return lower.contains("exception")
                || lower.contains("panic")
                || lower.contains("failed")
                || errorCount >= 3;
    }
}