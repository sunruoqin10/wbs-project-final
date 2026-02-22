package com.wbs.project.service;

import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 项目Service
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TaskService taskService;
    private final TaskMapper taskMapper;
    private final EmailNotificationService emailNotificationService;

    /**
     * 查询所有项目（包含成员信息）
     */
    public List<Project> getAllProjects() {
        List<Project> projects = projectMapper.selectAll();
        // 为每个项目加载成员列表
        projects.forEach(this::loadProjectMembers);
        return projects;
    }

    /**
     * 根据ID查询项目（包含成员信息）
     */
    public Project getProjectById(String id) {
        Project project = projectMapper.selectById(id);
        if (project != null) {
            loadProjectMembers(project);
        }
        return project;
    }

    /**
     * 根据状态查询项目列表（包含成员信息）
     */
    public List<Project> getProjectsByStatus(String status) {
        List<Project> projects = projectMapper.selectByStatus(status);
        projects.forEach(this::loadProjectMembers);
        return projects;
    }

    /**
     * 根据负责人ID查询项目列表（包含成员信息）
     */
    public List<Project> getProjectsByOwner(String ownerId) {
        List<Project> projects = projectMapper.selectByOwnerId(ownerId);
        projects.forEach(this::loadProjectMembers);
        return projects;
    }

    /**
     * 查询用户参与的项目（包含成员信息）
     */
    public List<Project> getProjectsByMember(String userId) {
        List<Project> projects = projectMapper.selectByMemberId(userId);
        projects.forEach(this::loadProjectMembers);
        return projects;
    }

    /**
     * 为项目加载成员列表
     */
    private void loadProjectMembers(Project project) {
        if (project != null) {
            List<String> memberIds = projectMemberMapper.selectMemberIdsByProjectId(project.getId());
            project.setMemberIds(memberIds);
        }
    }

    /**
     * 创建项目（包含成员关系）
     */
    @Transactional
    public Project createProject(Project project) {
        project.setId("p" + UUID.randomUUID().toString().substring(0, 8));
        project.setProgress(0);
        project.setCreatedAt(LocalDateTime.now());
        project.setUpdatedAt(LocalDateTime.now());

        // 先插入项目基本信息
        projectMapper.insert(project);

        // 然后处理项目成员关系
        if (project.getMemberIds() != null && !project.getMemberIds().isEmpty()) {
            updateProjectMembers(project.getId(), project.getMemberIds());
        }

        // 发送邮件通知
        if (project.getMemberIds() != null && !project.getMemberIds().isEmpty()) {
            emailNotificationService.notifyProjectCreated(project, project.getMemberIds());
        }

        return project;
    }

    /**
     * 更新项目（包含成员关系）
     */
    @Transactional
    public Project updateProject(String id, Project project) {
        Project existingProject = projectMapper.selectById(id);
        if (existingProject == null) {
            throw new RuntimeException("项目不存在");
        }

        // 获取旧的成员列表和状态
        List<String> oldMemberIds = existingProject.getMemberIds() != null ? 
            new java.util.ArrayList<>(existingProject.getMemberIds()) : new java.util.ArrayList<>();
        String oldStatus = existingProject.getStatus();

        project.setId(id);
        project.setUpdatedAt(LocalDateTime.now());

        // 更新项目基本信息
        projectMapper.update(project);

        // 处理项目成员关系（如果提供了成员列表）
        if (project.getMemberIds() != null) {
            updateProjectMembers(id, project.getMemberIds());
            
            // 发送成员变更通知
            Project updatedProject = getProjectById(id);
            sendMemberChangeNotifications(existingProject, oldMemberIds, updatedProject.getMemberIds());
        }

        // 检查状态变更并发送通知
        if (project.getStatus() != null && !project.getStatus().equals(oldStatus)) {
            Project updatedProject = getProjectById(id);
            if (updatedProject.getMemberIds() != null && !updatedProject.getMemberIds().isEmpty()) {
                emailNotificationService.notifyProjectStatusChanged(
                    updatedProject, oldStatus, project.getStatus(), updatedProject.getMemberIds());
            }
        }

        // 返回更新后的完整项目信息（包含成员）
        return getProjectById(id);
    }

    /**
     * 发送成员变更通知
     */
    private void sendMemberChangeNotifications(Project project, List<String> oldMemberIds, List<String> newMemberIds) {
        if (oldMemberIds == null) oldMemberIds = new java.util.ArrayList<>();
        if (newMemberIds == null) newMemberIds = new java.util.ArrayList<>();

        // 找出新增的成员
        for (String newMemberId : newMemberIds) {
            if (!oldMemberIds.contains(newMemberId)) {
                emailNotificationService.notifyMemberAdded(project, newMemberId);
            }
        }

        // 找出移除的成员
        for (String oldMemberId : oldMemberIds) {
            if (!newMemberIds.contains(oldMemberId)) {
                emailNotificationService.notifyMemberRemoved(project, oldMemberId);
            }
        }
    }

    /**
     * 更新项目成员关系
     * 先删除所有成员，再批量插入新成员
     */
    private void updateProjectMembers(String projectId, List<String> memberIds) {
        // 先删除该项目现有的所有成员关系
        projectMemberMapper.deleteByProjectId(projectId);

        // 批量插入新的成员关系
        if (memberIds != null && !memberIds.isEmpty()) {
            projectMemberMapper.batchInsert(projectId, memberIds);
        }
    }

    /**
     * 删除项目（级联删除相关数据）
     */
    @Transactional
    public void deleteProject(String id) {
        Project project = projectMapper.selectById(id);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 删除项目的所有成员关系
        projectMemberMapper.deleteByProjectId(id);

        // 删除项目的所有任务
        taskMapper.deleteByProjectId(id);

        // 最后删除项目本身
        projectMapper.deleteById(id);
    }

    /**
     * 获取项目总数
     */
    public int getTotalProjects() {
        return projectMapper.countTotal();
    }

    /**
     * 获取进行中的项目数
     */
    public int getActiveProjectsCount() {
        return projectMapper.countByStatus("active");
    }

    // ==================== 延期管理方法 ====================

    /**
     * 更新项目的延期状态（只统计叶子任务的延期）
     */
    public void updateProjectDelayedStatus(Project project) {
        if (project == null) {
            return;
        }

        // 获取项目的所有任务
        List<Task> allTasks = taskService.getTasksByProjectId(project.getId());
        if (allTasks == null || allTasks.isEmpty()) {
            // 如果没有任务，检查项目日期是否延期
            updateProjectDelayedStatusByDate(project);
            return;
        }

        // 计算所有任务的延期状态
        taskService.updateDelayedStatus(allTasks);

        // 只统计叶子任务的延期
        int delayedTasks = 0;
        int totalDelayedDays = 0;

        for (Task task : allTasks) {
            // 只统计叶子任务（没有子任务的任务）
            if (taskService.isLeafTask(task.getId())) {
                if (task.getIsDelayed() != null && task.getIsDelayed()) {
                    delayedTasks++;
                    if (task.getDelayedDays() != null) {
                        totalDelayedDays += task.getDelayedDays();
                    }
                }
            }
        }

        project.setDelayedTasks(delayedTasks);
        project.setTotalDelayedDays(totalDelayedDays);
        project.setIsDelayed(delayedTasks > 0);
    }

    /**
     * 更新项目的延期状态（基于项目日期）
     */
    private void updateProjectDelayedStatusByDate(Project project) {
        if (project.getEndDate() == null || "completed".equals(project.getStatus())) {
            project.setIsDelayed(false);
            project.setDelayedTasks(0);
            project.setTotalDelayedDays(0);
            return;
        }

        // 检查项目是否延期
        boolean isDelayed = project.getEndDate().isBefore(java.time.LocalDate.now());
        project.setIsDelayed(isDelayed);
        project.setDelayedTasks(0);
        project.setTotalDelayedDays(0);
    }

    /**
     * 批量更新项目的延期状态
     */
    public void updateProjectsDelayedStatus(List<Project> projects) {
        if (projects == null || projects.isEmpty()) {
            return;
        }

        for (Project project : projects) {
            updateProjectDelayedStatus(project);
        }
    }
}
