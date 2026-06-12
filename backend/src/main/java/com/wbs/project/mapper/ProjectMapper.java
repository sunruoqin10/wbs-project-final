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
}
