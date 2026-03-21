package org.archangel.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class Incident {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private String severity;

    @Column(length = 2000)
    private String summary;

    private String recommendedAction;

    private String executedCommand;

    private int exitCode;

    @Column(length = 4000)
    private String commandOutput;
}
