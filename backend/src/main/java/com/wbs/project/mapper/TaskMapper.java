package com.wbs.project.mapper;

import com.wbs.project.entity.Task;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 任务Mapper接口
 */
@Mapper
public interface TaskMapper {

    /**
     * 查询所有任务
     */
    List<Task> selectAll();

    /**
     * 根据ID查询任务
     */
    Task selectById(@Param("id") String id);

    /**
     * 根据项目ID查询任务列表
     */
    List<Task> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据父任务ID查询子任务列表
     */
    List<Task> selectByParentTaskId(@Param("parentTaskId") String parentTaskId);

    /**
     * 根据状态查询任务列表
     */
    List<Task> selectByStatus(@Param("status") String status);

    /**
     * 根据分配人ID查询任务列表
     */
    List<Task> selectByAssigneeId(@Param("assigneeId") String assigneeId);

    /**
     * 插入任务
     */
    int insert(Task task);

    /**
     * 更新任务
     */
    int update(Task task);

    /**
     * 删除任务
     */
    int deleteById(@Param("id") String id);

    /**
     * 统计任务总数
     */
    int countTotal();

    /**
     * 统计指定项目的任务数
     */
    int countByProjectId(@Param("projectId") String projectId);

    /**
     * 统计指定状态的任务数
     */
    int countByStatus(@Param("status") String status);
}
