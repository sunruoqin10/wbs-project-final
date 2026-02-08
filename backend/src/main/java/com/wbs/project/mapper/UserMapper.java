package com.wbs.project.mapper;

import com.wbs.project.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户Mapper接口
 */
@Mapper
public interface UserMapper {

    /**
     * 查询所有用户
     */
    List<User> selectAll();

    /**
     * 根据ID查询用户
     */
    User selectById(@Param("id") String id);

    /**
     * 根据邮箱查询用户
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 根据用户名查询用户
     */
    User selectByName(@Param("name") String name);

    /**
     * 根据角色查询用户列表
     */
    List<User> selectByRole(@Param("role") String role);

    /**
     * 根据部门查询用户列表
     */
    List<User> selectByDepartment(@Param("department") String department);

    /**
     * 根据ID列表查询用户
     */
    List<User> selectByIds(@Param("ids") List<String> ids);

    /**
     * 插入用户
     */
    int insert(User user);

    /**
     * 更新用户
     */
    int update(User user);

    /**
     * 删除用户
     */
    int deleteById(@Param("id") String id);

    /**
     * 统计用户总数
     */
    int countTotal();
}
