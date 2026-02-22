package com.wbs.project.controller;

import com.wbs.project.annotation.RequirePermission;
import com.wbs.project.common.Result;
import com.wbs.project.entity.Project;
import com.wbs.project.service.PermissionService;
import com.wbs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        projectService.updateProjectsDelayedStatus(projects);
        return Result.success(projects);
    }

    @GetMapping("/{id}")
    public Result<Project> getProjectById(@PathVariable String id) {
        Project project = projectService.getProjectById(id);
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
    @RequirePermission("project:create")
    public Result<Project> createProject(@RequestBody Project project) {
        try {
            Project createdProject = projectService.createProject(project);
            return Result.success("项目创建成功", createdProject);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Project> updateProject(
            @PathVariable String id, 
            @RequestBody Project project,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            if (userId != null && !permissionService.isProjectOwner(userId, id)) {
                return Result.error("无权限编辑此项目");
            }
            Project updatedProject = projectService.updateProject(id, project);
            projectService.updateProjectDelayedStatus(updatedProject);
            return Result.success("项目更新成功", updatedProject);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        try {
            if ("admin".equals(userRole)) {
                projectService.deleteProject(id);
                return Result.success();
            }
            if (userId != null && permissionService.isProjectOwner(userId, id)) {
                projectService.deleteProject(id);
                return Result.success();
            }
            return Result.error("无权限删除此项目");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
