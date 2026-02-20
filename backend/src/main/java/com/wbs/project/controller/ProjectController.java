package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Project;
import com.wbs.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 项目Controller
 */
@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 获取所有项目
     * GET /api/projects
     */
    @GetMapping
    public Result<List<Project>> getAllProjects() {
        List<Project> projects = projectService.getAllProjects();
        projectService.updateProjectsDelayedStatus(projects); // 计算延期状态
        return Result.success(projects);
    }

    /**
     * 根据ID获取项目
     * GET /api/projects/{id}
     */
    @GetMapping("/{id}")
    public Result<Project> getProjectById(@PathVariable String id) {
        Project project = projectService.getProjectById(id);
        if (project == null) {
            return Result.error("项目不存在");
        }
        projectService.updateProjectDelayedStatus(project); // 计算延期状态
        return Result.success(project);
    }

    /**
     * 根据状态获取项目列表
     * GET /api/projects/status/{status}
     */
    @GetMapping("/status/{status}")
    public Result<List<Project>> getProjectsByStatus(@PathVariable String status) {
        List<Project> projects = projectService.getProjectsByStatus(status);
        projectService.updateProjectsDelayedStatus(projects); // 计算延期状态
        return Result.success(projects);
    }

    /**
     * 根据负责人获取项目列表
     * GET /api/projects/owner/{ownerId}
     */
    @GetMapping("/owner/{ownerId}")
    public Result<List<Project>> getProjectsByOwner(@PathVariable String ownerId) {
        List<Project> projects = projectService.getProjectsByOwner(ownerId);
        projectService.updateProjectsDelayedStatus(projects); // 计算延期状态
        return Result.success(projects);
    }

    /**
     * 获取用户参与的项目
     * GET /api/projects/member/{userId}
     */
    @GetMapping("/member/{userId}")
    public Result<List<Project>> getProjectsByMember(@PathVariable String userId) {
        List<Project> projects = projectService.getProjectsByMember(userId);
        projectService.updateProjectsDelayedStatus(projects); // 计算延期状态
        return Result.success(projects);
    }

    /**
     * 创建项目
     * POST /api/projects
     */
    @PostMapping
    public Result<Project> createProject(@RequestBody Project project) {
        try {
            Project createdProject = projectService.createProject(project);
            return Result.success("项目创建成功", createdProject);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新项目
     * PUT /api/projects/{id}
     */
    @PutMapping("/{id}")
    public Result<Project> updateProject(@PathVariable String id, @RequestBody Project project) {
        try {
            Project updatedProject = projectService.updateProject(id, project);
            projectService.updateProjectDelayedStatus(updatedProject); // 计算延期状态
            return Result.success("项目更新成功", updatedProject);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除项目
     * DELETE /api/projects/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteProject(@PathVariable String id) {
        try {
            projectService.deleteProject(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取项目统计信息
     * GET /api/projects/stats
     */
    @GetMapping("/stats")
    public Result<ProjectStats> getProjectStats() {
        ProjectStats stats = new ProjectStats();
        stats.setTotalProjects(projectService.getTotalProjects());
        stats.setActiveProjects(projectService.getActiveProjectsCount());
        return Result.success(stats);
    }

    /**
     * 项目统计信息类
     */
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
