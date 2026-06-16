package com.wbs.project.mapper;

import com.wbs.project.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目Mapper接口
 */
@Mapper
public interface ProjectMapper {

    /**
     * 查询所有项目
     */
    List<Project> selectAll();

    /**
     * 根据ID查询项目
     */
    Project selectById(@Param("id") String id);

    /**
     * 根据状态查询项目列表
     */
    List<Project> selectByStatus(@Param("status") String status);

    /**
     * 根据负责人ID查询项目列表
     */
    List<Project> selectByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 查询用户参与的项目列表（作为成员）
     */
    List<Project> selectByMemberId(@Param("userId") String userId);

    /**
     * 插入项目
     */
    int insert(Project project);

    /**
     * 更新项目
     */
    int update(Project project);

    /**
     * 删除项目
     */
    int deleteById(@Param("id") String id);

    /**
     * 统计项目总数
     */
    int countTotal();

    /**
     * 统计进行中的项目数
     */
    int countByStatus(@Param("status") String status);

    /**
     * 根据部门编码列表查询项目（角色管理 v2：部门项目负责人数据范围）
     * 适用于 dept_code IN (deptCode1, deptCode2, ...)
     */
    List<Project> selectByDeptCodes(@Param("deptCodes") java.util.List<String> deptCodes);

    /**
     * 根据 ID 集合批量查询项目（角色管理 v2：成员/owner 数据范围）
     */
    List<Project> selectByIds(@Param("ids") java.util.List<String> ids);

    /**
     * 根据 ownerId 集合查询项目 ID 列表（角色管理 v2：project-manager 数据范围）
     */
    java.util.List<String> selectIdsByOwner(@Param("ownerId") String ownerId);

    /**
     * 根据 createdBy 集合查询项目 ID 列表（创建者数据范围——创建者始终可访问自己创建的项目）
     */
    java.util.List<String> selectIdsByCreatedBy(@Param("createdBy") String createdBy);

    /**
     * 根据 PM 的 managed_project_ids 查询项目 ID 列表(2026-06-12 新增)
     * 注意:managed_project_ids 是 JSON 数组,用 JSON_CONTAINS 匹配
     */
    java.util.List<String> selectIdsByManagedProjectIds(@Param("userId") String userId);

    // ============ PM / Dept-PM 变更交接(2026-06-16) ============

    /**
     * 可交接项目:owner_id = #{ownerId} 且未完成且未冻结
     */
    List<Project> selectHandoverableByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 行级锁(SELECT ... FOR UPDATE)用于交接事务
     */
    List<Project> lockByIds(@Param("ids") java.util.List<String> ids);

    /**
     * needs_handover=1 项目按角色过滤可见性:
     * - admin: 全部
     * - dept-project-manager: 仅 dept_code IN managedDeptCodes
     * - 其它: 空
     */
    List<Project> selectNeedsHandoverVisibleTo(@Param("role") String role,
                                                @Param("managedDeptCodes") java.util.List<String> managedDeptCodes);

    /**
     * 部门(们)下未完成项目的 owner_id 列表(去重)— 部门合并通知用
     */
    java.util.List<String> selectPmIdsByDeptCodes(@Param("deptCodes") java.util.List<String> deptCodes);

    /**
     * DTO 专用:仅 select 6 字段给 HandoverPreviewResponse.ProjectBrief
     */
    java.util.List<com.wbs.project.dto.ProjectBriefRow> selectProjectBriefRowsByOwnerId(@Param("ownerId") String ownerId);

    /**
     * 部门编码下未完成项目数
     */
    int countActiveByDeptCode(@Param("deptCode") String deptCode);

    /**
     * 部门合并:把 dept_code = oldCode 的未完成项目改 newCode
     */
    int updateDeptCodeForActiveProjects(@Param("oldCode") String oldCode,
                                        @Param("newCode") String newCode);

    /**
     * 部门(们)下按状态过滤的项目 ID 列表(部门合并审计用)
     */
    java.util.List<String> selectIdsByDeptCodesAndStatus(
        @Param("deptCodes") java.util.List<String> deptCodes,
        @Param("excludeStatuses") java.util.List<String> excludeStatuses,
        @Param("exclude") boolean exclude);

    /**
     * owner 名下未完成项目 ID 列表(离职冻结用)
     */
    java.util.List<String> selectIdsByActiveOwner(@Param("ownerId") String ownerId);

    /**
     * 部门(们)下未完成项目标记 needs_handover=1(离职冻结用)
     */
    int markNeedsHandoverByDeptCodes(@Param("deptCodes") java.util.List<String> deptCodes);
}
