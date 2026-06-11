package com.wbs.project.mapper;

import com.wbs.project.entity.RoleChangeLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色变更审计 Mapper
 * 对应表 sys_role_change_log
 */
@Mapper
public interface RoleChangeLogMapper {

    /**
     * 插入一条角色变更记录
     * @return 插入行数
     */
    int insert(RoleChangeLog log);

    /**
     * 根据被操作用户 ID 查询变更历史（按时间倒序）
     */
    List<RoleChangeLog> selectByUserId(@Param("userId") String userId);
}
