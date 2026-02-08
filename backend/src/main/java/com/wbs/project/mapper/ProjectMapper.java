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
}
