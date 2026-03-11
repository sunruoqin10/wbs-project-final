package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeeklyReportComment {
    private String id;
    private String reportId;
    private String userId;
    private String content;
    private LocalDateTime createdAt;
}
