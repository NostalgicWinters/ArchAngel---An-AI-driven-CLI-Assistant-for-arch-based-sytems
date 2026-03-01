package org.archangel.model;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@ApplicationScoped
@AllArgsConstructor
@NoArgsConstructor
@Data
public class CommandResponse
{
    private String command;
    private int exitCode;
    private String stdout;
    private String stderr;
}
