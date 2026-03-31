package org.archangel.think;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload sent to the Python brain service.
 *
 * FIXED: Previously, LogicInterface sent a raw String as a JSON body.
 * MicroProfile REST Client serializes a plain String as a JSON primitive ("..."),
 * not an object — Python's Pydantic model expected {"logs": "..."} and got "..."
 * causing a 422 Unprocessable Entity on every single AI call.
 *
 * This wrapper ensures the wire format matches AnalyzeRequest in brain/main.py exactly.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyzeRequest {

    @JsonProperty("logs")
    private String logs;

    @JsonProperty("source")
    private String source;

    public static AnalyzeRequest fromJournal(String logs) {
        return new AnalyzeRequest(logs, "journalctl");
    }
}