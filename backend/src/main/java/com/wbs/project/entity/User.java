package com.wbs.project.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class User {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private String role;
    private String department;
    private String skills; // JSON字符串存储技能列表
    private String password; // 明文密码
    private LocalDateTime joinedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // === HR 同步扩展字段（2026-06-10/06-11 来自 MDM 中间表） ===
    private String deptCode;     // 部门 code（来自 mdm_if_pa_a.ORG_CD）
    private String subOrgCd;     // 父部门 code（来自 mdm_if_pa_a.SUB_ORG_CD）
    private String subOrgNam;    // 父部门名（来自 mdm_if_pa_a.SUB_ORG_NAM）
    private String companyCd;    // 公司 code（来自 mdm_if_pa_a.COMPANY_CD，2700/8400）
    private String chineseNam;   // 中文姓名（来自 mdm_if_pa_a.CHINESE_NAM）
    private String status = "C"; // 在职状态: C=在职, H=休职, T=离职（来自 mdm_if_pa_a.ACT_CLSS_CD）

    // === 角色管理 v2 扩展字段(2026-06-11) ===
    private String managedDeptCodes;  // JSON 字符串,该用户作为部门项目负责人管理的部门编码列表(仅 role=dept-project-manager 有效)
    private String managedCompanyCd;  // 该用户作为部门项目负责人的公司编码(需与 user.companyCd 一致)
    private String managedProjectIds; // JSON 字符串,该用户作为项目经理管理的项目 ID 列表(仅 role=project-manager 有效,2026-06-12 新增)
    private Integer tokenVersion = 0; // JWT 版本号,角色/管辖范围变更时 +1,AuthInterceptor 校验不一致则 401
}
