package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

/**
 * 角色变更请求 DTO
 * 由 admin 在 Team 页面提交，PUT /api/users/{id}/role
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleChangeRequest {
    private String newRole;                 // 新角色(必填)
    private List<String> managedDeptCodes;  // 管辖部门编码列表(仅 newRole=dept-project-manager 有效)
    private String managedCompanyCd;        // 管辖公司编码(仅 newRole=dept-project-manager 必填)
    private String reason;                  // 变更原因(可选,便于审计)
}
