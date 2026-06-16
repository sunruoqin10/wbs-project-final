package com.wbs.project.dto;
import lombok.Data;
import java.util.List;
@Data
public class HandoverRequest {
    private String handoverType = "PM_HANDOVER";
    private String successorUserId;
    private List<String> projectIds;
    private String reason;
}
