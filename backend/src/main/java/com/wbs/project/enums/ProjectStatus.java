package com.wbs.project.enums;

import lombok.Getter;

/**
 * 项目状态枚举
 */
@Getter
public enum ProjectStatus {
    PLANNING("planning", "计划中"),
    ACTIVE("active", "进行中"),
    COMPLETED("completed", "已完成"),
    ON_HOLD("on-hold", "已暂停");

    private final String code;
    private final String description;

    ProjectStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return PLANNING;
    }
}
