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

    /**
     * 查询以指定前缀开头的最大用户ID
     * 用于生成自增的C0000001格式ID
     */
    String selectMaxIdByPrefix(@Param("prefix") String prefix);

    /**
     * 关键词搜索用户（支持分页）
     */
    List<User> searchUsers(@Param("keyword") String keyword,
                           @Param("offset") int offset,
                           @Param("pageSize") int pageSize);

    /**
     * 关键词搜索用户总数
     */
    int countSearchUsers(@Param("keyword") String keyword);

    /**
     * 同步人事数据：插入新用户
     * @return 插入行数
     */
    int syncHrInsert();

    /**
     * 同步人事数据：更新已有用户
     * @return 更新行数
     */
    int syncHrUpdate();

    /**
     * 同步人事数据：标记已离职用户 status='T'
     * 条件：MDM 中不存在 或 MDM 状态为 T
     * @return 更新的行数
     */
    int syncHrMarkResigned();

    /**
     * 统计未在 MDM 中记录的管理员数
     * 用作同步前误标保护
     * @return 管理员但无 MDM 记录的条数
     */
    int countAdminNotInMdm();

    /**
     * 查询未在 MDM 中记录的管理员 ID 列表
     * 用于同步前误标保护，把具体 ID 暴露给运维
     * @return 管理员 ID 列表（如 ["C0000001", "C0000007"]），空列表表示无风险
     */
    java.util.List<String> selectAdminIdsNotInMdm();

    /**
     * 更新用户角色与管辖范围（角色管理 v2）
     * 同步 bump token_version，强制 token 失效
     * @return 更新行数
     */
    int updateRoleAndScope(@Param("id") String id,
                           @Param("role") String role,
                           @Param("managedDeptCodes") String managedDeptCodes,
                           @Param("managedCompanyCd") String managedCompanyCd);

    /**
     * 仅 bump token_version（用于旧 project-manager 兼容期降级）
     * @return 更新行数
     */
    int bumpTokenVersion(@Param("id") String id);
}
