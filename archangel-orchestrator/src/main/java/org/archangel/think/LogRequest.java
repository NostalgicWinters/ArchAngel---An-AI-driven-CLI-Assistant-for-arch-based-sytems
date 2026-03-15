package org.archangel.think;

import lombok.Data;

@Data
public class LogRequest {

    private String logs;
    private String hostname;
    private long timestamp;
}