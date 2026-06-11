package com.wbs.project.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 *
 * 5 个角色 + 数据范围：
 * - ADMIN               : 全员
 * - DEPT_PROJECT_MANAGER: 我所管理部门(dept_code IN managed_dept_codes)
 * - PROJECT_MANAGER     : 我 owner 的项目(owner_id == self)。已废弃,迁移后会清空,保留枚举兼容
 * - MEMBER              : 我参与的项目(member_id == self)
 * - VIEWER              : 我参与的项目(只读)
 */
@Getter
public enum UserRole {
    ADMIN("admin", "管理员"),
    DEPT_PROJECT_MANAGER("dept-project-manager", "部门项目负责人"),
    @Deprecated
    PROJECT_MANAGER("project-manager", "项目负责人(已废弃)"),
    MEMBER("member", "项目人员"),
    VIEWER("viewer", "观察者");

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
