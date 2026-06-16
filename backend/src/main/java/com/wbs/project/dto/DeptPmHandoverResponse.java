package com.wbs.project.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
@Data
@AllArgsConstructor
public class DeptPmHandoverResponse {
    private int handoveredDeptCount;
    private List<String> deptCodes;
}
