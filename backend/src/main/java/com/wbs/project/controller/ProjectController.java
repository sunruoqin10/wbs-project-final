package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.service.PermissionService;
import com.wbs.project.service.ProjectService;
import com.wbs.project.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final PermissionService permissionService;
    private final ProjectMapper projectMapper;
    private final UserService userService;

    /**
     * 查询所有项目（角色管理 v2：按当前用户数据范围过滤）
     */
    @GetMapping
    public Result<List<Project>> getAllProjects(HttpServletRequest request) {
        String currentUserId = (String) request.getAttribute("userId");
        List<Project> projects = projectService.getAllProjectsForUser(currentUserId);
        projectService.updateProjectsDelayedStatus(projects);
        return Result.success(projects);
    }

    /**
     * 待指派项目列表(2026-06-16 新增,spec §2.6)
     * GET /api/projects/needs-handover
     * 自动按 caller 角色过滤：admin: 全部;dept-pm: 本部门;其它: 空
     */
    @GetMapping("/needs-handover")
    public Result<List<Project>> listNeedsHandover(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        User caller = userService.getUserById(userId);
        List<String> managedCodes = null;
        if (caller != null && "dept-project-manager".equals(caller.getRole())) {
            managedCodes = userService.parseManagedDeptCodes(caller.getManagedDeptCodes());
        }
        String role = caller == null ? "" : caller.getRole();
        List<Project> list = projectMapper.selectNeedsHandoverVisibleTo(role, managedCodes);
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<Project> getProjectById(@PathVariable String id) {
        Project project = projectService.getProjectByIdWithProgressUpdate(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        projectService.updateProjectDelayedStatus(project);
        return Result.success(project);
    }

    @GetMapping("/status/{status}")
    public Result<List<Project>> getProjectsByStatus(@PathVariable String status) {
        List<Project> projects = projectService.getProjectsByStatus(status);
        projectService.updateProjectsDelayedStatus(projects);
        return Result.success(projects);
    }

    @GetMapping("/owner/{ownerId}")
    public Result<List<Project>> getProjectsByOwner(@PathVariable String ownerId) {
        List<Project> projects = projectService.getProjectsByOwner(ownerId);
        projectService.updateProjectsDelayedStatus(projects);
        return Result.success(projects);
    }

    @GetMapping("/member/{userId}")
    public Result<List<Project>> getProjectsByMember(@PathVariable String userId) {
        List<Project> projects = projectService.getProjectsByMember(userId);
        projectService.updateProjectsDelayedStatus(projects);
        return Result.success(projects);
    }

    @PostMapping
    public Result<Project> createProject(@RequestBody Project project, HttpServletRequest request) {
        String currentUserId = (String) request.getAttribute("userId");
        // 角色管理 v2:权限校验
        if (!permissionService.canCreateProject(currentUserId)) {
            return Result.error(403, "无创建项目权限");
        }
        // dept-project-manager 必须在所管部门下创建
        if (permissionService.isDeptProjectManager(currentUserId)) {
            if (project.getDeptCode() == null
                    || !permissionService.isDeptManager(currentUserId, project.getDeptCode())) {
                return Result.error(403, "项目部门必须为您管辖的部门");
            }
        }
        // 记录创建者(独立于 owner / members),供数据范围和 UI 展示
        if (currentUserId != null) {
            project.setCreatedBy(currentUserId);
        }
        Project createdProject = projectService.createProject(project);
        return Result.success("项目创建成功", createdProject);
    }

    @PutMapping("/{id}")
    public Result<Project> updateProject(
            @PathVariable String id,
            @RequestBody Project project,
            HttpServletRequest request) {
        String currentUserId = (String) request.getAttribute("userId");
        if (!permissionService.canEditProject(currentUserId, id)) {
            return Result.error(403, "无项目编辑权限");
        }
        Project updatedProject = projectService.updateProject(id, project);
        projectService.updateProjectDelayedStatus(updatedProject);
        return Result.success("项目更新成功", updatedProject);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(
            @PathVariable String id,
            HttpServletRequest request) {
        String currentUserId = (String) request.getAttribute("userId");
        if (!permissionService.canEditProject(currentUserId, id)) {
            return Result.error(403, "无项目删除权限");
        }
        projectService.deleteProject(id);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<ProjectStats> getProjectStats() {
        ProjectStats stats = new ProjectStats();
        stats.setTotalProjects(projectService.getTotalProjects());
        stats.setActiveProjects(projectService.getActiveProjectsCount());
        return Result.success(stats);
    }

    public static class ProjectStats {
        private Integer totalProjects;
        private Integer activeProjects;

        public Integer getTotalProjects() {
            return totalProjects;
        }

        public void setTotalProjects(Integer totalProjects) {
            this.totalProjects = totalProjects;
        }

        public Integer getActiveProjects() {
            return activeProjects;
        }

        public void setActiveProjects(Integer activeProjects) {
            this.activeProjects = activeProjects;
        }
    }
}
