package com.wbs.project.service;

import com.wbs.project.entity.Project;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
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

        project.setId(id);
        project.setUpdatedAt(LocalDateTime.now());

        // 更新项目基本信息
        projectMapper.update(project);

        // 处理项目成员关系（如果提供了成员列表）
        if (project.getMemberIds() != null) {
            updateProjectMembers(id, project.getMemberIds());
        }

        // 返回更新后的完整项目信息（包含成员）
        return getProjectById(id);
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

        // TODO: 删除项目的所有任务
        // TODO: 删除项目的所有标签

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
}
