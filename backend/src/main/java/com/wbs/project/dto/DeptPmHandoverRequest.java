package com.wbs.project.dto;
import lombok.Data;
import java.util.List;
@Data
public class DeptPmHandoverRequest {
    private String handoverType = "DEPT_PM_HANDOVER";
    private String successorUserId;
    private List<String> deptCodes;
    private String reason;
}
