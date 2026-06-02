package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchedulerConfig {
    private String id;
    private String name;
    private String description;
    private String cronExpression;
    private Boolean enabled;
    private LocalDateTime lastRunTime;
    private LocalDateTime nextRunTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
