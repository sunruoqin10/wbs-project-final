package com.wbs.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbs.project.entity.Permission;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.enums.UserRole;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.PermissionMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 权限 Service（角色管理 v2 重写）
 *
 * 4 角色 × 5 档数据范围集中实现。Controller / Service / AOP 都通过本类判定权限，
 * 避免散落 if (admin) 判断。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<String, Set<String>> rolePermissionCache = new ConcurrentHashMap<>();

    // ============ 元数据查询（沿用旧实现） ============

    public List<Permission> getAllPermissions() {
        return permissionMapper.selectAll();
    }

    public List<Permission> getPermissionsByRole(String role) {
        return permissionMapper.selectByRole(role);
    }

    public boolean hasPermission(String role, String permissionCode) {
        if (role == null || permissionCode == null) {
            return false;
        }
        Set<String> permissions = rolePermissionCache.computeIfAbsent(role, r -> {
            List<String> codes = permissionMapper.selectPermissionCodesByRole(r);
            return new HashSet<>(codes);
        });
        return permissions.contains(permissionCode);
    }

    /**
     * 项目级权限检查（兼容旧 PermissionController 接口）
     * 项目 owner / 项目成员 / 部门项目负责人(部门内) / admin 视为有权限
     */
    public boolean hasProjectPermission(String userId, String projectId, String permissionCode) {
        if (userId == null || projectId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        if (isProjectOwner(userId, projectId)) {
            return true;
        }
        if (isProjectMember(userId, projectId)) {
            return true;
        }
        if (isProjectInManagedDept(userId, projectId)) {
            return true;
        }
        return false;
    }

    public void refreshCache() {
        rolePermissionCache.clear();
    }

    public void refreshCache(String role) {
        rolePermissionCache.remove(role);
    }

    // ============ 角色快捷判断 ============

    public boolean isAdmin(String userId) {
        return getRole(userId).map(r -> UserRole.ADMIN.code.equals(r)).orElse(false);
    }

    public boolean isDeptProjectManager(String userId) {
        return getRole(userId).map(r -> UserRole.DEPT_PROJECT_MANAGER.code.equals(r)).orElse(false);
    }

    public boolean isProjectManager(String userId) {
        return getRole(userId).map(r -> UserRole.PROJECT_MANAGER.code.equals(r)).orElse(false);
    }

    public boolean isMember(String userId) {
        return getRole(userId).map(r -> UserRole.MEMBER.code.equals(r)).orElse(false);
    }

    public boolean isViewer(String userId) {
        return getRole(userId).map(r -> UserRole.VIEWER.code.equals(r)).orElse(false);
    }

    // ============ 数据范围判断（核心） ============

    /**
     * 是否能查看该项目
     * 规则：admin 全通;dept-pm 看所管部门项目;pm 看 owner 项目;member/viewer 看参与项目
     */
    public boolean canViewProject(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        // 项目负责人(owner) 可看
        if (userId.equals(project.getOwnerId())) {
            return true;
        }
        // 项目成员 可看
        if (isProjectMember(userId, projectId)) {
            return true;
        }
        // 部门项目负责人 且 项目归属部门 在所管部门内
        if (isDeptProjectManager(userId) && project.getDeptCode() != null
                && isDeptManager(userId, project.getDeptCode())) {
            return true;
        }
        return false;
    }

    /**
     * 是否能编辑该项目（标题/日期/负责人/部门/成员/状态）
     * 规则：admin / dept-pm(部门内) / pm(owner) 允许;其他禁止
     */
    public boolean canEditProject(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }
        if (isProjectOwner(userId, projectId)) {
            return true;
        }
        if (isDeptProjectManager(userId) && project.getDeptCode() != null
                && isDeptManager(userId, project.getDeptCode())) {
            return true;
        }
        return false;
    }

    /**
     * 是否能创建项目
     * 规则：admin 任意;dept-pm 必须有所管部门;其他禁止
     */
    public boolean canCreateProject(String userId) {
        if (isAdmin(userId)) {
            return true;
        }
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            return u != null && hasManagedDept(u);
        }
        return false;
    }

    /**
     * 是否能在该项目下创建任务
     */
    public boolean canCreateTask(String userId, String projectId) {
        return canEditProject(userId, projectId);
    }

    /**
     * 是否能编辑任务(任意字段)
     * 规则同 canEditProject
     */
    public boolean canEditTask(String userId, String taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        // 通过 taskId 找 projectId
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        String projectId = task.getProjectId();
        if (isProjectOwner(userId, projectId)) {
            return true;
        }
        if (isDeptProjectManager(userId)) {
            Project project = projectMapper.selectById(projectId);
            return project != null && project.getDeptCode() != null
                    && isDeptManager(userId, project.getDeptCode());
        }
        return false;
    }

    /**
     * 是否能编辑任务的 progress 字段
     * 规则：
     * - admin / dept-pm(部门内) / pm(owner) 全部允许
     * - member: 仅当 task.assigneeId == self
     * - viewer: 不允许
     */
    public boolean canEditTaskProgress(String userId, String taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        String projectId = task.getProjectId();
        // owner / dept-pm 部门内 / pm(owner) 允许
        if (canEditProject(userId, projectId)) {
            return true;
        }
        // member: 仅当 assigneeId == self
        if (isMember(userId) && userId.equals(task.getAssigneeId())) {
            return true;
        }
        return false;
    }

    /**
     * 是否能查看任务
     */
    public boolean canViewTask(String userId, String taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        if (isAdmin(userId)) {
            return true;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return canViewProject(userId, task.getProjectId());
    }

    /**
     * 是否能查看周报
     */
    public boolean canViewWeeklyReport(String userId, String reportId) {
        return canViewProject(userId, resolveProjectIdFromReport(reportId))
                || isReportSubmitter(userId, reportId);
    }

    /**
     * 是否能审批周报
     */
    public boolean canApproveWeeklyReport(String userId, String reportId) {
        if (isAdmin(userId)) {
            return true;
        }
        String projectId = resolveProjectIdFromReport(reportId);
        if (projectId == null) {
            return false;
        }
        return canEditProject(userId, projectId);
    }

    /**
     * 是否能审批加班记录
     */
    public boolean canApproveOvertime(String userId, String overtimeId) {
        if (isAdmin(userId)) {
            return true;
        }
        // 简化：依赖 OvertimeService.hasPermission 已有逻辑
        // 此处只做粗粒度判断
        return isDeptProjectManager(userId) || isProjectManager(userId);
    }

    /**
     * 是否能编辑文档(上传/删除)
     */
    public boolean canEditDocument(String userId, String projectId) {
        return canEditProject(userId, projectId);
    }

    /**
     * 是否能创建/修改其他用户角色
     */
    public boolean canManageUserRole(String userId) {
        return isAdmin(userId);
    }

    // ============ 项目 owner / member 判断（沿用） ============

    public boolean isProjectOwner(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        return project != null && userId.equals(project.getOwnerId());
    }

    public boolean isProjectMember(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        return projectMemberMapper.isProjectMember(projectId, userId);
    }

    // ============ 部门管理 ============

    /**
     * 给定用户是否管理指定 dept_code
     */
    public boolean isDeptManager(String userId, String deptCode) {
        if (userId == null || deptCode == null) {
            return false;
        }
        if (!isDeptProjectManager(userId)) {
            return false;
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return false;
        }
        List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
        return codes.contains(deptCode);
    }

    /**
     * 给定用户是否管理该项目所在部门
     */
    public boolean isProjectInManagedDept(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        if (!isDeptProjectManager(userId)) {
            return false;
        }
        Project project = projectMapper.selectById(projectId);
        return project != null && project.getDeptCode() != null
                && isDeptManager(userId, project.getDeptCode());
    }

    // ============ 工具方法 ============

    private java.util.Optional<String> getRole(String userId) {
        if (userId == null) {
            return java.util.Optional.empty();
        }
        User u = userMapper.selectById(userId);
        return u == null ? java.util.Optional.empty() : java.util.Optional.ofNullable(u.getRole());
    }

    private List<String> parseDeptCodes(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 managed_dept_codes JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private boolean hasManagedDept(User u) {
        List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
        return !codes.isEmpty();
    }

    private String resolveProjectIdFromReport(String reportId) {
        // 简化:由调用方在 Service 中提供 projectId,本类不直接查周报表
        // 调用方应使用 canViewProject(userId, projectId)
        return null;
    }

    private boolean isReportSubmitter(String userId, String reportId) {
        // 简化:同上,真实判断在 Service 层做
        return false;
    }

    // ============ 鉴权抛出 ============

    public void requireAdmin(String userId) {
        if (!isAdmin(userId)) {
            throw new BusinessException(403, "需要管理员权限");
        }
    }

    public void requireProjectEditable(String userId, String projectId) {
        if (!canEditProject(userId, projectId)) {
            throw new BusinessException(403, "无项目编辑权限");
        }
    }

    public void requireTaskEditable(String userId, String taskId) {
        if (!canEditTask(userId, taskId)) {
            throw new BusinessException(403, "无任务编辑权限");
        }
    }

    public void requireTaskProgressEditable(String userId, String taskId) {
        if (!canEditTaskProgress(userId, taskId)) {
            throw new BusinessException(403, "只能修改自己负责的任务进度");
        }
    }

    /**
     * 解析项目范围(用于 ProjectService.getProjects)
     */
    public List<String> getAccessibleProjectIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isAdmin(userId)) {
            return null; // null 表示全部
        }
        Set<String> ids = new HashSet<>();
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
                List<Project> ps = codes.isEmpty() ? Collections.emptyList() : projectMapper.selectByDeptCodes(codes);
                ps.forEach(p -> ids.add(p.getId()));
            }
            return new ArrayList<>(ids);
        }
        // pm / member / viewer: 参与的项目
        ids.addAll(projectMemberMapper.selectProjectIdsByUserId(userId));
        // pm 还要加上 owner 的项目
        if (isProjectManager(userId)) {
            ids.addAll(projectMapper.selectIdsByOwner(userId));
        }
        return new ArrayList<>(ids);
    }

    /**
     * 解析用户的 managed_dept_codes JSON 字段
     * 供 ProjectService.getProjectsByManagedDepts 等调用
     */
    public List<String> parseManagedDeptCodes(User u) {
        if (u == null) {
            return Collections.emptyList();
        }
        return parseDeptCodes(u.getManagedDeptCodes());
    }
}
