package org.archangel.system;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessResult {
    private int exitCode;
    private String stdout;
    private String stderr;
    private boolean timedOut;
}