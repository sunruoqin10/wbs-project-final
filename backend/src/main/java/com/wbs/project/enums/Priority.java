package com.wbs.project.enums;

import lombok.Getter;

/**
 * 优先级枚举
 */
@Getter
public enum Priority {
    LOW("low", "低"),
    MEDIUM("medium", "中"),
    HIGH("high", "高"),
    URGENT("urgent", "紧急"),
    CRITICAL("critical", "严重");

    private final String code;
    private final String description;

    Priority(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static Priority fromCode(String code) {
        for (Priority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        return MEDIUM;
    }
}
