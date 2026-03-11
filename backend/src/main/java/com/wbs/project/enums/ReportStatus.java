package com.wbs.project.enums;

import lombok.Getter;

@Getter
public enum ReportStatus {
    DRAFT("draft", "草稿"),
    SUBMITTED("submitted", "已提交"),
    APPROVED("approved", "已审批"),
    REJECTED("rejected", "已拒绝");

    private final String code;
    private final String description;

    ReportStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ReportStatus fromCode(String code) {
        for (ReportStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return DRAFT;
    }
}
