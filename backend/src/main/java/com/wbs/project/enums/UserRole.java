package com.wbs.project.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    ADMIN("admin", "管理员"),
    PROJECT_MANAGER("project-manager", "项目经理"),
    MEMBER("member", "成员"),
    VIEWER("viewer", "查看者");

    private final String code;
    private final String description;

    UserRole(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static UserRole fromCode(String code) {
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        return MEMBER;
    }
}
