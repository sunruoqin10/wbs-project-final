package com.wbs.project.dto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProjectHandoverLogDTO {
    private Long id;
    private String projectId;
    private String projectName;
    private String handoverType;
    private String fromUserId;
    private String fromUserName;
    private String toUserId;
    private String toUserName;
    private String fromDeptCode;
    private String toDeptCode;
    private String reason;
    private String operatorId;
    private String operatorName;
    private LocalDateTime createdAt;
}
