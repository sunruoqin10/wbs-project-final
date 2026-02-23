package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelayNotificationRecord {
    private String id;
    private String taskId;
    private String projectId;
    private String notifiedUserId;
    private String notifiedEmail;
    private LocalDate notificationDate;
    private LocalDateTime createdAt;
}
