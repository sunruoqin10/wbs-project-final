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
    // === HR 同步扩展字段（2026-06-10 来自 MDM 中间表） ===
    private String deptCode;     // 部门 code（来自 mdm_if_pa_a.ORG_CD）
    private String subOrgCd;     // 父部门 code（来自 mdm_if_pa_a.SUB_ORG_CD）
    private String subOrgNam;    // 父部门名（来自 mdm_if_pa_a.SUB_ORG_NAM）
    private String companyCd;    // 公司 code（来自 mdm_if_pa_a.COMPANY_CD，2700/8400）
    private String chineseNam;   // 中文姓名（来自 mdm_if_pa_a.CHINESE_NAM）
}
