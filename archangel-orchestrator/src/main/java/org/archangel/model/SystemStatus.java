package org.archangel.model;
import lombok.Data;

@Data
public class SystemStatus {
    private boolean executionEnabled;
    private long totalIncidents;
    private String lastSeverity;
    private String lastSummary;
}
