package com.wbs.project.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class DocumentAccessLog {
    private String id;
    private String documentId;
    private String userId;
    private String action;
    private String ipAddress;
    private String userAgent;
    private LocalDateTime createdAt;
}
