package com.wbs.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbs.project.entity.Document;
import com.wbs.project.entity.Permission;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.enums.UserRole;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.PermissionMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.mapper.WeeklyReportMapper;
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
    private final WeeklyReportMapper weeklyReportMapper;
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
     * 规则：admin 全通;创建者 总可看;dept-pm 看所管部门项目;pm 看 owner 项目;member/viewer 看参与项目
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
        // 创建者 始终可看(独立于 owner / member)
        if (userId.equals(project.getCreatedBy())) {
            return true;
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
     * 规则：admin / 创建者 / dept-pm(部门内) / pm(managed_projects) 允许;其他禁止(2026-06-12 PM 加)
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
        // 创建者 始终可编辑
        if (userId.equals(project.getCreatedBy())) {
            return true;
        }
        if (isProjectOwner(userId, projectId)) {
            return true;
        }
        if (isDeptProjectManager(userId) && project.getDeptCode() != null
                && isDeptManager(userId, project.getDeptCode())) {
            return true;
        }
        if (isManagedProject(userId, projectId)) {
            return true;
        }
        return false;
    }

    /**
     * 任务内容管理权限(创建/编辑/删除任务标题/描述/负责人/状态/日期等):
     * admin + 项目创建者 + 项目负责人(owner) + 项目经理(project in managed_project_ids)。
     * 部门项目负责人不能再管任务(2026-06-12 收紧);2026-06-12 加入 PM 分支。
     */
    public boolean canManageTaskContent(String userId, String projectId) {
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
        if (userId.equals(project.getCreatedBy())) {
            return true;
        }
        if (userId.equals(project.getOwnerId())) {
            return true;
        }
        if (isProjectManager(userId) && isManagedProject(userId, projectId)) {
            return true;
        }
        return false;
    }

    /**
     * 是否能在该项目下创建任务
     * 规则：admin / 项目创建者 / 项目负责人(owner)
     */
    public boolean canCreateTask(String userId, String projectId) {
        return canManageTaskContent(userId, projectId);
    }

    /**
     * 是否能在指定父任务下创建子任务
     * 规则：admin / 项目创建者 / 项目负责人(owner) / 父任务的 assignee
     * (2026-06-12:任务的负责人可为自己的任务添加子任务)
     */
    public boolean canAddSubtask(String userId, String parentTaskId) {
        if (userId == null || parentTaskId == null) {
            return false;
        }
        Task parent = taskMapper.selectById(parentTaskId);
        if (parent == null) {
            return false;
        }
        if (canManageTaskContent(userId, parent.getProjectId())) {
            return true;
        }
        if (userId.equals(parent.getAssigneeId())) {
            return true;
        }
        return false;
    }

    /**
     * 是否能编辑任务(任意字段)
     * 规则：admin / 项目创建者 / 项目负责人(owner) / 任务的 assignee(任务负责人可编辑自己的任务)
     * 任务进度调整走 canEditTaskProgress,不受此影响
     */
    public boolean canEditTask(String userId, String taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        if (canManageTaskContent(userId, task.getProjectId())) {
            return true;
        }
        if (userId.equals(task.getAssigneeId())) {
            return true;
        }
        return false;
    }

    /**
     * 是否能删除任务
     * 规则：admin / 项目创建者 / 项目负责人(owner) / 任务的 assignee
     * (2026-06-12:任务负责人可删除自己的任务)
     */
    public boolean canDeleteTask(String userId, String taskId) {
        if (userId == null || taskId == null) {
            return false;
        }
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        if (canManageTaskContent(userId, task.getProjectId())) {
            return true;
        }
        if (userId.equals(task.getAssigneeId())) {
            return true;
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
        WeeklyReport report = weeklyReportMapper.selectById(reportId);
        if (report == null) return false;

        // 2026-06-14 规则:草稿状态对 creator 之外的人不可见(严格解读,admin 也受此约束)
        if ("draft".equals(report.getStatus())) {
            return userId != null && userId.equals(report.getUserId());
        }

        // 非草稿:admin 放行 + creator 放行 + 数据范围
        if (isAdmin(userId)) return true;
        if (userId != null && userId.equals(report.getUserId())) return true;

        // submitter 不在可见范围内 → 拒绝
        User submitter = userMapper.selectById(report.getUserId());
        if (submitter == null) return false;
        String submitterDept = submitter.getDeptCode();
        String projectId = report.getProjectId();

        if (isProjectOwner(userId, projectId)) return true;
        if (isManagedProject(userId, projectId)) return true;
        if (isDeptManager(userId, submitterDept)) return true;
        return false;
    }

    /**
     * 是否能审批周报
     */
    public boolean canApproveWeeklyReport(String approverId, String reportId) {
        User approver = userMapper.selectById(approverId);
        if (approver == null) return false;
        if ("admin".equals(approver.getRole())) return true;

        WeeklyReport report = weeklyReportMapper.selectById(reportId);
        if (report == null) return false;

        // 防自审:任何身份都不能审批自己提交的周报
        if (approverId.equals(report.getUserId())) return false;

        User submitter = userMapper.selectById(report.getUserId());
        if (submitter == null) return false;

        String submitterRole = submitter.getRole();
        String submitterDept = submitter.getDeptCode();
        String projectId = report.getProjectId();

        // 档 ②:提交者是 PM / dept-pm → 仅同部门 dept-pm 可批(防 PM 互批)
        if ("project-manager".equals(submitterRole)
                || "dept-project-manager".equals(submitterRole)) {
            return isDeptManager(approverId, submitterDept);
        }

        // 档 ②-bis(2026-06-14 调整):提交者是项目 owner(非 PM/dept-pm 身份)
        // → 同部门 dept-pm 或 项目内 PM(via managed_project_ids)可批
        if (isProjectOwner(submitter.getId(), projectId)) {
            if (isDeptManager(approverId, submitterDept)) return true;
            if (isManagedProject(approverId, projectId)) return true;
            return false;
        }

        // 档 ③-⑤:普通成员提交
        if (isProjectOwner(approverId, projectId)) return true;
        if (isManagedProject(approverId, projectId)) return true;
        if (isDeptManager(approverId, submitterDept)) return true;
        return false;
    }

    /**
     * 是否能编辑周报（仅本人 + draft/rejected）
     */
    public boolean canEditWeeklyReport(String userId, String reportId) {
        if (isAdmin(userId)) return true;
        WeeklyReport report = weeklyReportMapper.selectById(reportId);
        if (report == null) return false;
        if (!userId.equals(report.getUserId())) return false;       // 仅本人
        String s = report.getStatus();
        return "draft".equals(s) || "rejected".equals(s);
    }

    /**
     * 是否能删除周报（仅本人 + draft）
     */
    public boolean canDeleteWeeklyReport(String userId, String reportId) {
        if (isAdmin(userId)) return true;
        WeeklyReport report = weeklyReportMapper.selectById(reportId);
        if (report == null) return false;
        if (!userId.equals(report.getUserId())) return false;
        return "draft".equals(report.getStatus());
    }

    /**
     * 当前用户能查看的「周报提交者」userId 集合。
     * - admin 返回 null（不限）
     * - 自己始终能看
     * - dept-project-manager → 所辖部门的所有在职用户
     * - project-manager → managed + owner 项目的所有成员
     * - 普通项目 owner（member 角色但拥有项目）→ 其 owner 项目的成员
     * - 其他（member/viewer）→ 仅自己
     */
    public List<String> getAccessibleWeeklyReportUserIds(String userId) {
        User u = userMapper.selectById(userId);
        if (u == null) return Collections.emptyList();
        if ("admin".equals(u.getRole())) return null;        // 不限

        Set<String> ids = new HashSet<>();
        ids.add(userId);                                      // 自己始终能看

        if ("dept-project-manager".equals(u.getRole())) {
            List<String> depts = parseManagedDeptCodes(u);    // 已存在的 helper
            if (!depts.isEmpty()) {
                ids.addAll(userMapper.selectIdsByDeptCodes(depts));
            }
        }
        if ("project-manager".equals(u.getRole())) {
            List<String> pids = parseManagedProjectIds(u);    // 已存在的 helper
            List<String> ownerPids = projectMapper.selectIdsByOwner(userId);
            Set<String> projectIds = new HashSet<>();
            projectIds.addAll(pids);
            projectIds.addAll(ownerPids);
            if (!projectIds.isEmpty()) {
                ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(
                        new ArrayList<>(projectIds)));
            }
        } else {
            // 非 PM 的普通 owner:role 不是 PM 但有 owner 关系
            // (上面 PM 分支已合并 owner 项目,这里不重复调用 selectIdsByOwner)
            List<String> ownedProjects = projectMapper.selectIdsByOwner(userId);
            if (!ownedProjects.isEmpty()) {
                ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(ownedProjects));
            }
        }

        return new ArrayList<>(ids);
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

    // ============ 项目经理管理(2026-06-12) ============

    /**
     * 给定用户(项目经理)是否管理指定 projectId
     * 数据源: sys_user.managed_project_ids (JSON 数组)
     */
    public boolean isManagedProject(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        if (!isProjectManager(userId)) {
            return false;
        }
        User u = userMapper.selectById(userId);
        if (u == null) {
            return false;
        }
        List<String> ids = parseProjectIds(u.getManagedProjectIds());
        return ids.contains(projectId);
    }

    /**
     * 是否能创建项目
     * 规则: admin / dept-pm(managed_dept_codes) / pm(自己 dept_code) - 2026-06-12
     */
    public boolean canCreateProject(String userId) {
        if (isAdmin(userId)) {
            return true;
        }
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            return u != null && hasManagedDept(u);
        }
        if (isProjectManager(userId)) {
            // PM 必须有 dept_code(来自 HR 同步);有 dept 才能归属
            User u = userMapper.selectById(userId);
            return u != null && u.getDeptCode() != null && !u.getDeptCode().isEmpty();
        }
        return false;
    }

    /**
     * 是否能改其他用户角色
     * 规则: admin 任意 / dept-pm 仅可改本部门内 member/viewer/project-manager(不能动 admin 和 dept-pm,也不能把人提为 dept-pm)
     */
    public boolean canChangeRoleInDept(String actorId, String targetUserId, String newRole) {
        if (actorId == null || targetUserId == null) {
            return false;
        }
        if (isAdmin(actorId)) {
            return true;
        }
        if (!isDeptProjectManager(actorId)) {
            return false;
        }
        if (newRole == null) {
            return false;
        }
        // dept-pm 只能赋予 project-manager / member / viewer（不能提为 admin 或 dept-pm）
        if (UserRole.ADMIN.code.equals(newRole) || UserRole.DEPT_PROJECT_MANAGER.code.equals(newRole)) {
            return false;
        }
        // 目标用户必须在本部门内
        User target = userMapper.selectById(targetUserId);
        if (target == null || target.getDeptCode() == null || target.getDeptCode().isEmpty()) {
            return false;
        }
        if (!isDeptManager(actorId, target.getDeptCode())) {
            return false;
        }
        // 目标用户当前角色不能是 admin 或 dept-project-manager（dept-pm 无权操作同级/上级角色）
        if (UserRole.ADMIN.code.equals(target.getRole()) || UserRole.DEPT_PROJECT_MANAGER.code.equals(target.getRole())) {
            return false;
        }
        return true;
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

    /**
     * 解析 managed_project_ids JSON 字段(2026-06-12 新增)
     * 失败或为空返回空列表
     */
    private List<String> parseProjectIds(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 managed_project_ids JSON 失败: {}", json, e);
            return Collections.emptyList();
        }
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
     * 创建者创建的项目始终在范围内,与其当前角色无关
     */
    public List<String> getAccessibleProjectIds(String userId) {
        if (userId == null) {
            return Collections.emptyList();
        }
        if (isAdmin(userId)) {
            return null; // null 表示全部
        }
        Set<String> ids = new HashSet<>();
        // 创建者:始终包含自己创建的项目
        ids.addAll(projectMapper.selectIdsByCreatedBy(userId));
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
                List<Project> ps = codes.isEmpty() ? Collections.emptyList() : projectMapper.selectByDeptCodes(codes);
                ps.forEach(p -> ids.add(p.getId()));
            }
            return new ArrayList<>(ids);
        }
        if (isProjectManager(userId)) {
            // PM(2026-06-12):managed_project_ids 内的项目
            ids.addAll(projectMapper.selectIdsByManagedProjectIds(userId));
            return new ArrayList<>(ids);
        }
        // member / viewer: 参与的项目
        ids.addAll(projectMemberMapper.selectProjectIdsByUserId(userId));
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

    /**
     * 解析用户的 managed_project_ids JSON 字段(2026-06-12)
     * 失败或为空返回空列表
     */
    public List<String> parseManagedProjectIds(User u) {
        if (u == null) {
            return Collections.emptyList();
        }
        return parseProjectIds(u.getManagedProjectIds());
    }

    // ============ 文档权限数据范围（2026-06-13） ============

    /**
     * 返回当前用户能看/管的「文档上传者」userId 集合。
     * admin 返回 null（语义：不限）；MEMBER/VIEWER 兜底含自己 + 参与项目的成员。
     */
    public Set<String> getAccessibleUploaderIds(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        if (isAdmin(userId)) {
            return null;
        }
        Set<String> ids = new HashSet<>();
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
                if (!codes.isEmpty()) {
                    ids.addAll(userMapper.selectIdsByDeptCodes(codes));
                }
            }
            return ids;
        }
        if (isProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> managed = parseProjectIds(u.getManagedProjectIds());
                if (!managed.isEmpty()) {
                    ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(managed));
                }
            }
            ids.add(userId); // 自己上传的总能看到
            return ids;
        }
        // 项目负责人 / MEMBER / VIEWER 兜底
        List<String> ownerProjects = projectMapper.selectIdsByOwner(userId);
        if (!ownerProjects.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(ownerProjects));
        }
        List<String> participated = projectMemberMapper.selectProjectIdsByUserId(userId);
        if (!participated.isEmpty()) {
            ids.addAll(projectMemberMapper.selectMemberIdsByProjectIds(participated));
        }
        ids.add(userId);
        return ids;
    }

    /**
     * 返回当前用户能看/管的「文档所属项目」projectId 集合。
     * admin 返回 null（不限）；DEPT_PM 不按项目维度管辖，返回空集合。
     */
    public Set<String> getAccessibleProjectIdsForDoc(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        if (isAdmin(userId)) {
            return null;
        }
        if (isProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u == null) return Collections.emptySet();
            return new HashSet<>(parseProjectIds(u.getManagedProjectIds()));
        }
        if (isDeptProjectManager(userId)) {
            return Collections.emptySet(); // 部门 PM 走 uploader 维度,projectIds 留空 → SQL 的 <if> 守卫跳过
        }
        Set<String> ids = new HashSet<>(projectMapper.selectIdsByOwner(userId));
        ids.addAll(projectMemberMapper.selectProjectIdsByUserId(userId));
        return ids;
    }

    // ============ 加班权限数据范围（2026-06-13） ============

    /**
     * 返回当前用户能查看的「加班记录提交者」userId 集合。
     * 与 getAccessibleUploaderIds 同形（对齐 dept-pm 维度），但语义是"可见的加班来源"。
     *
     * 规则（2026-06-14 修订）：
     *  - admin 返回 null（不限）
     *  - project-manager → 其作为 owner 的所有项目的成员 ID
     *  - dept-project-manager → 所辖部门内的在职用户 ID 集合
     *  - 项目负责人（owner 但非 admin/PM/dept-pm）→ 其作为 owner 的所有项目的成员 ID
     *  - 其他（MEMBER / VIEWER）→ 仅自己
     *
     * 业务方应这样用：对 null 跳过 IN 守卫，非空 IN (...) 守卫，空集合 → 直接返回空结果。
     */
    public Set<String> getAccessibleOvertimeUserIds(String userId) {
        if (userId == null) {
            return Collections.emptySet();
        }
        if (isAdmin(userId)) {
            return null; // admin = 不限
        }
        if (isDeptProjectManager(userId)) {
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> codes = parseDeptCodes(u.getManagedDeptCodes());
                if (!codes.isEmpty()) {
                    return new HashSet<>(userMapper.selectIdsByDeptCodes(codes));
                }
            }
            return Collections.emptySet();
        }
        // 2026-06-14: project-manager: 其 owner 的项目 + managed_project_ids 中的项目的所有成员 ID
        // 确保项目经理能看到由他管理/创建的项目中所有成员的加班信息
        if (isProjectManager(userId)) {
            Set<String> allMemberIds = new HashSet<>();
            // owner 项目的成员
            allMemberIds.addAll(getOwnerProjectMemberIds(userId));
            // managed_project_ids 项目的成员
            User u = userMapper.selectById(userId);
            if (u != null) {
                List<String> managedIds = parseProjectIds(u.getManagedProjectIds());
                if (!managedIds.isEmpty()) {
                    List<String> managedMemberIds = projectMemberMapper.selectMemberIdsByProjectIds(managedIds);
                    if (managedMemberIds != null) {
                        allMemberIds.addAll(managedMemberIds);
                    }
                }
            }
            allMemberIds.add(userId); // 确保能看到自己的记录
            if (!allMemberIds.isEmpty()) {
                return allMemberIds;
            }
            return Set.of(userId);
        }

        // 普通项目 owner(非 project-manager): 其负责的所有项目的成员 ID
        Set<String> ownerProjectMemberIds = getOwnerProjectMemberIds(userId);
        if (!ownerProjectMemberIds.isEmpty()) {
            return ownerProjectMemberIds;
        }
        // 兜底: 仅自己
        return Set.of(userId);
    }

    /**
     * 获取用户作为 owner 的所有项目的成员 userId 集合（含自身）
     */
    private Set<String> getOwnerProjectMemberIds(String userId) {
        List<String> ownerProjectIds = projectMapper.selectIdsByOwner(userId);
        if (ownerProjectIds == null || ownerProjectIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<String> memberIds = projectMemberMapper.selectMemberIdsByProjectIds(ownerProjectIds);
        Set<String> result = new HashSet<>();
        if (memberIds != null) {
            result.addAll(memberIds);
        }
        result.add(userId); // 确保能看到自己的记录
        return result;
    }

    // ============ 文档权限单文档判定（2026-06-13） ============

    /**
     * 是否能查看指定文档（list/detail/download/preview 共用）
     */
    public boolean canViewDocument(String userId, Document doc) {
        if (userId == null || doc == null) {
            return false;
        }
        if (isAdmin(userId)) return true;
        // 自上传统一早返回（覆盖所有角色）
        if (userId.equals(doc.getUploadedBy())) return true;

        // 部门 PM:上传者 dept 在 managed_dept_codes 内（含 general）
        User uploader = userMapper.selectById(doc.getUploadedBy());
        if (isDeptProjectManager(userId) && uploader != null
                && isDeptManager(userId, uploader.getDeptCode())) {
            return true;
        }
        // PM:doc.projectId ∈ managedProjectIds 且 uploader 是项目成员
        if (isProjectManager(userId) && doc.getProjectId() != null
                && isManagedProject(userId, doc.getProjectId())
                && isProjectMember(doc.getUploadedBy(), doc.getProjectId())) {
            return true;
        }
        // 项目负责人:project.ownerId == self 且 uploader 是项目成员
        if (doc.getProjectId() != null) {
            Project p = projectMapper.selectById(doc.getProjectId());
            if (p != null && userId.equals(p.getOwnerId())
                    && isProjectMember(doc.getUploadedBy(), doc.getProjectId())) {
                return true;
            }
            // MEMBER / VIEWER 兜底:参与的项目
            if (isProjectMember(userId, doc.getProjectId())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 删除权限(2026-06-13 业务确认)
     * 规则:删除权严格弱于查看权,按"上传者在我范围内的角色"判定——
     *  1. admin 全部
     *  2. 自上传 始终可删
     *  3. 部门 PM:上传者 dept ∈ managed_dept_codes(含 general)
     *  4. 项目 PM:doc.projectId ∈ managed_project_ids 且 上传者是该项目成员
     *  5. 项目 owner:project.ownerId = self 且 上传者是该项目成员
     *  6. MEMBER:仅自上传(其他成员上传不可删)
     *  7. VIEWER:无删除权
     */
    public boolean canDeleteDocument(String userId, Document doc) {
        if (userId == null || doc == null) {
            return false;
        }
        if (isAdmin(userId)) return true;
        // 自上传 可删(覆盖 dept-pm / PM / owner / MEMBER);
        // VIEWER 无任何删除权(spec 矩阵),连自上传也不行
        if (userId.equals(doc.getUploadedBy()) && !isViewer(userId)) return true;

        // 部门 PM:上传者所在部门在 managed_dept_codes 内(含 general 上传者按其 dept_code 判)
        if (isDeptProjectManager(userId)) {
            User uploader = userMapper.selectById(doc.getUploadedBy());
            return uploader != null
                    && uploader.getDeptCode() != null
                    && isDeptManager(userId, uploader.getDeptCode());
        }

        // 项目 PM:仅在 doc.projectId 非空 + 项目在 managed_project_ids 内 +
        //              上传者是该项目成员 时可删
        if (isProjectManager(userId) && doc.getProjectId() != null) {
            return isManagedProject(userId, doc.getProjectId())
                    && isProjectMember(doc.getUploadedBy(), doc.getProjectId());
        }

        // 项目 owner:仅当上传者是 owner 自己拥有项目的成员时可删
        if (doc.getProjectId() != null) {
            Project p = projectMapper.selectById(doc.getProjectId());
            if (p != null && userId.equals(p.getOwnerId())) {
                return isProjectMember(doc.getUploadedBy(), doc.getProjectId());
            }
        }

        // MEMBER:仅自上传,已在前面 early return;其他情况 false
        // VIEWER:无删除权
        return false;
    }

    /**
     * 上传权限校验
     */
    public boolean canUploadDocument(String userId, String projectId) {
        if (userId == null) return false;
        if (isAdmin(userId)) return true;
        if (projectId == null) return true; // general 任何人可上传
        if (isDeptProjectManager(userId)) {
            Project p = projectMapper.selectById(projectId);
            return p != null && p.getDeptCode() != null
                    && isDeptManager(userId, p.getDeptCode());
        }
        if (isProjectManager(userId)) {
            return isManagedProject(userId, projectId);
        }
        // 项目负责人 / MEMBER 兜底
        if (isProjectMember(userId, projectId)) {
            // MEMBER 通过此分支放行;VIEWER 由 Controller 角色白名单另外拒
            Project p = projectMapper.selectById(projectId);
            if (p != null && userId.equals(p.getOwnerId())) return true;
            // MEMBER 但非 owner:仅自己参与项目可上传
            return !isViewer(userId);
        }
        return false;
    }

    /**
     * 加班记录关联任务权限校验 (2026-06-13;2026-06-14 增 PM 分支)
     * 规则:
     * - admin / dept-project-manager / member / viewer: 只能为自己负责的
     *   任务(task.assigneeId === userId)记加班。
     * - project-manager: 可选项目内任意未完成叶子任务(2026-06-14 业务调整:
     *   PM 需要为所管项目整体加班统计负责,可代为申请)。
     * - taskId 为空时(无关联任务)放行。
     * 与前端 OvertimeModal.projectTasks 过滤逻辑保持一致,
     * 避免 API 直调绕过。
     */
    public boolean canCreateOvertimeOnTask(String userId, String taskId) {
        if (userId == null) return false;
        if (taskId == null || taskId.isEmpty()) return true; // 无关联任务放行
        Task task = taskMapper.selectById(taskId);
        if (task == null) return false; // 任务不存在

        // 2026-06-14: 项目经理可选任意任务(为整体项目加班统计负责)
        if (isProjectManager(userId)) {
            return true;
        }

        return userId.equals(task.getAssigneeId());
    }

    /** 抛错版本：用于 upload/delete/update 入口 */
    public void requireViewDocument(String userId, Document doc) {
        if (!canViewDocument(userId, doc)) {
            throw new BusinessException(403, "无文档查看权限");
        }
    }

    public void requireDeleteDocument(String userId, Document doc) {
        if (!canDeleteDocument(userId, doc)) {
            throw new BusinessException(403, "无文档删除权限");
        }
    }

    public void requireUploadDocument(String userId, String projectId) {
        if (!canUploadDocument(userId, projectId)) {
            throw new BusinessException(403, "无文档上传权限");
        }
    }
}
