package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 角色变更审计记录
 * 对应表 sys_role_change_log
 * 由 admin 在 Team 页面修改某用户角色时写入
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RoleChangeLog {
    private Long id;
    private String userId;                  // 被操作用户 id
    private String oldRole;                 // 变更前角色
    private String newRole;                 // 变更后角色
    private String oldManagedDeptCodes;     // 变更前管辖部门编码(JSON 数组字符串)
    private String newManagedDeptCodes;     // 变更后管辖部门编码(JSON 数组字符串)
    private String oldManagedCompanyCd;     // 变更前管辖公司编码
    private String newManagedCompanyCd;     // 变更后管辖公司编码
    private String oldManagedProjectIds;     // 变更前管辖项目 ID(JSON 数组字符串)
    private String newManagedProjectIds;     // 变更后管辖项目 ID(JSON 数组字符串)
    private String changedBy;               // 操作人 id
    private LocalDateTime changedAt;        // 操作时间
    private String reason;                  // 变更原因
}
