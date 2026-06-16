package com.wbs.project.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
@Data
@AllArgsConstructor
public class DeptMergeResponse {
    private int affectedProjectCount;
    private String newDeptCode;
}
