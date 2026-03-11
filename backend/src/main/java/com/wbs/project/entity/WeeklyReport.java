package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyReport {
    private String id;
    private String userId;
    private String projectId;
    private LocalDate weekStart;
    private LocalDate weekEnd;
    private String completedWork;
    private String nextWeekPlan;
    private String problems;
    private String status;
    private LocalDateTime submitTime;
    private LocalDateTime approveTime;
    private String approverId;
    private String approveComment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
