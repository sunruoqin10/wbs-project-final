package com.wbs.project.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 *
 * 4 个角色 + 数据范围(2026-06-12 重写 PM 角色;2026-06-17 移除 VIEWER):
 * - ADMIN               : 全员
 * - DEPT_PROJECT_MANAGER: 我所管理部门(dept_code IN managed_dept_codes);可指派 PM
 * - PROJECT_MANAGER     : 我所管项目(id IN managed_project_ids);PM 之间互不可见
 * - MEMBER              : 我参与的项目(member_id == self)
 */
@Getter
public enum UserRole {
    ADMIN("admin", "管理员"),
    DEPT_PROJECT_MANAGER("dept-project-manager", "部门项目负责人"),
    PROJECT_MANAGER("project-manager", "项目经理"),
    MEMBER("member", "项目人员");

    /** 数据库存储值（公开以支持外部直接 .code 访问，避免 .getCode() 冗余） */
    public final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserRole fromCode(String code) {
        if (code == null) {
            return MEMBER;
        }
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return MEMBER;
    }

    /**
     * 是否为管理员
     */
    public static boolean isAdmin(String code) {
        return ADMIN.code.equals(code);
    }

    /**
     * 是否为部门项目负责人
     */
    public static boolean isDeptProjectManager(String code) {
        return DEPT_PROJECT_MANAGER.code.equals(code);
    }
}
