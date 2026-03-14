package com.wbs.project.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Document {
    private String id;
    private String projectId;
    private String taskId;
    private String name;
    private String category;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String fileExtension;
    private Integer version;
    private String parentId;
    private String description;
    private String uploadedBy;
    private String status;
    private Integer downloadCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
