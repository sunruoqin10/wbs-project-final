package com.wbs.project.annotation;

/**
 * 数据范围枚举（角色管理 v2）
 * 用于 @RequirePermission 标注数据范围类型
 */
public enum DataScope {
    /** 全局权限码（不绑定数据范围） */
    GLOBAL,
    /** 项目级（需要 projectId） */
    PROJECT,
    /** 部门级（需要 deptCode） */
    DEPT,
    /** 仅自己（需要 userId） */
    SELF,
    /** 任务进度（需要 taskId,PermissionService 会校验 assigneeId==self） */
    PROGRESS
}
