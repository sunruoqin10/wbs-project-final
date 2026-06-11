package com.wbs.project.annotation;

import java.lang.annotation.*;

/**
 * 权限注解（角色管理 v2 扩展）
 * 用法:
 *   @RequirePermission(value = "user:create")          // 角色权限码
 *   @RequirePermission(value = "project:edit", dataScope = DataScope.PROJECT, projectIdParam = "id")
 *   @RequirePermission(value = "task:edit", dataScope = DataScope.PROJECT, projectIdParam = "projectId")
 *   @RequirePermission(value = "task:edit-progress", dataScope = DataScope.PROGRESS, projectIdParam = "id")
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    /** 权限码 */
    String value();

    /**
     * 数据范围（决定 PermissionAspect 用何种 PermissionService 方法判定）
     * - GLOBAL:  hasPermission(role, value)
     * - PROJECT: canViewProject / canEditProject,需配合 projectIdParam 读取 projectId
     * - DEPT:   isDeptManager,需配合 deptCodeParam 读取 deptCode
     * - SELF:   仅本人,需配合 selfParam 读取 userId
     * - PROGRESS: canEditTaskProgress,需配合 taskIdParam
     */
    DataScope dataScope() default DataScope.GLOBAL;

    /** projectId 参数名（dataScope=PROJECT 时必填） */
    String projectIdParam() default "";

    /** taskId 参数名（dataScope=PROGRESS 时必填） */
    String taskIdParam() default "";

    /** deptCode 参数名（dataScope=DEPT 时必填） */
    String deptCodeParam() default "";

    /** self userId 参数名（dataScope=SELF 时必填） */
    String selfParam() default "";
}
