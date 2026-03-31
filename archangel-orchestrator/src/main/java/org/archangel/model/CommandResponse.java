package org.archangel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandResponse {
    private String command;
    private int exitCode;
    private String stdout;
    private String stderr;
}