package com.wbs.project.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 附件实体类
 */
@Data
public class Attachment {
    private String id;
    private String taskId;
    private String name;
    private String url;
    private Long size;
    private String type;
    private String uploadedBy;
    private LocalDateTime uploadedAt;
}
