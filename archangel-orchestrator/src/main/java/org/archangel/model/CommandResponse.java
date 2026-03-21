package org.archangel.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FIXED: @ApplicationScoped REMOVED.
 *
 * Having @ApplicationScoped on a Lombok @Data model caused CDI to create a single
 * proxy instance shared across all requests. Under concurrent load, two threads
 * writing to the same proxy instance would corrupt each other's response data —
 * one request could receive another request's stdout/stderr.
 *
 * Model classes are plain data carriers. They must NOT be CDI beans.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandResponse {
    private String command;
    private int exitCode;
    private String stdout;
    private String stderr;
}