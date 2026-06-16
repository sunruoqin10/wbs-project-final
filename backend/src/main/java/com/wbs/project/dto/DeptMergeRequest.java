package com.wbs.project.dto;
import lombok.Data;
@Data
public class DeptMergeRequest {
    private String oldDeptCode;
    private String newDeptCode;
    private String successorDeptManagerId;
    private String reason;
}
