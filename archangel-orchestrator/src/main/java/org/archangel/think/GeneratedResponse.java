package org.archangel.think;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedResponse {

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL, UNKNOWN;

        @JsonCreator
        public static Severity fromString(String value) {
            if (value == null) return UNKNOWN;
            try {
                return Severity.valueOf(value.toUpperCase().trim());
            } catch (IllegalArgumentException e) {
                return UNKNOWN;
            }
        }

        @JsonValue
        public String toJson() {
            return this.name();
        }
    }

    private Severity severity = Severity.UNKNOWN;
    private String summary;
    private String recommendedAction;

    public boolean isAnalysisValid() {
        return severity != Severity.UNKNOWN
                && summary != null && !summary.isBlank()
                && recommendedAction != null && !recommendedAction.isBlank();
    }
}