package com.wbs.project.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 项目成员Mapper接口
 */
@Mapper
public interface ProjectMemberMapper {

    /**
     * 根据项目ID查询成员ID列表
     */
    List<String> selectMemberIdsByProjectId(@Param("projectId") String projectId);

    /**
     * 根据用户ID查询参与的项目ID列表
     */
    List<String> selectProjectIdsByUserId(@Param("userId") String userId);

    /**
     * 插入项目成员关系
     */
    int insert(@Param("projectId") String projectId, @Param("userId") String userId);

    /**
     * 根据项目ID删除所有成员关系
     */
    int deleteByProjectId(@Param("projectId") String projectId);

    /**
     * 删除指定的项目成员关系
     */
    int delete(@Param("projectId") String projectId, @Param("userId") String userId);

    /**
     * 批量插入项目成员
     */
    int batchInsert(@Param("projectId") String projectId, @Param("userIds") List<String> userIds);
}
