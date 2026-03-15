package org.archangel.automate;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Instant;
import java.util.ArrayDeque;

@ApplicationScoped
public class CommandRateLimit {
    private static final int MAX_COMMANDS = 10;

    private final ArrayDeque<Long> executions = new ArrayDeque<>();

    public synchronized boolean isAllowed()
    {
        long now = Instant.now().getEpochSecond();

        while (!executions.isEmpty() && now - executions.peekFirst() > 3600) {
            executions.pollFirst();
        }

        if (executions.size() >= MAX_COMMANDS) {
            return false;
        }

        executions.addLast(now);
        return true;
    }
}
