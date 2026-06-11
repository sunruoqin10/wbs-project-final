package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户Service
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    /**
     * 根据ID查询用户
     */
    public User getUserById(String id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据邮箱查询用户
     */
    public User getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /**
     * 根据ID列表查询用户
     */
    public List<User> getUsersByIds(List<String> ids) {
        return userMapper.selectByIds(ids);
    }

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        // 验证邮箱唯一性
        User existingUser = userMapper.selectByEmail(user.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        user.setId(generateNextUserId());
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        user.setJoinedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 生成下一个用户ID（C0000001, C0000002, ... 格式）
     */
    private String generateNextUserId() {
        final String PREFIX = "C";

        String maxId = userMapper.selectMaxIdByPrefix(PREFIX);
        long nextNum = 1L;
        if (maxId != null && maxId.length() > PREFIX.length()) {
            try {
                String numPart = maxId.substring(PREFIX.length());
                nextNum = Long.parseLong(numPart) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1L;
            }
        }
        return String.format("%s%07d", PREFIX, nextNum);
    }

    /**
     * 更新用户
     */
    @Transactional
    public User updateUser(String id, User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 如果修改邮箱，验证新邮箱的唯一性
        /* 
        if (!existingUser.getEmail().equals(user.getEmail())) {
            User emailUser = userMapper.selectByEmail(user.getEmail());
            if (emailUser != null) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        */

        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return userMapper.selectById(id);
    }

    /**
     * 删除用户
     * 注意：需要级联删除相关数据（在Java代码中处理）
     */
    @Transactional
    public void deleteUser(String id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // TODO: 在这里处理级联删除逻辑
        // 1. 删除用户相关的项目成员关系
        // 2. 删除用户相关的任务分配关系
        // 3. 删除用户相关的评论
        // 4. 删除用户上传的附件

        userMapper.deleteById(id);
    }

    /**
     * 获取用户总数
     */
    public int getTotalUsers() {
        return userMapper.countTotal();
    }

    /**
     * 用户登录（明文密码验证 - 使用用户ID）
     */
    public User login(String userId, String password) {
        // 根据用户ID查询
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 明文密码比对
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(String id, String currentPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!currentPassword.equals(user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        user.setPassword(newPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
    }

    /**
     * 获取所有项目经理
     */
    public List<User> getManagers() {
        return userMapper.selectByRole("project-manager");
    }

    /**
     * 关键词搜索用户（支持分页）
     */
    public java.util.Map<String, Object> searchUsers(String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<User> records = userMapper.searchUsers(keyword, offset, pageSize);
        int total = userMapper.countSearchUsers(keyword);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /**
     * 同步人事数据
     * 从 mdm_if_pa_a 和 mdm_if_or_a 表同步数据到 sys_user
     * 流程：插入新用户 → 更新已有用户 → 标记离职用户
     * 同步前会阻断「在岗 admin 但 MDM 缺失」的情况，避免误标管理员为离职
     * @return 包含 inserted / updated / resigned 数量的 Map
     */
    @Transactional
    public java.util.Map<String, Integer> syncHrData() {
        // 1. 同步前安全检查：阻断 admin 误标
        java.util.List<String> adminIds = userMapper.selectAdminIdsNotInMdm();
        if (!adminIds.isEmpty()) {
            throw new BusinessException(
                409,
                "检测到 " + adminIds.size() + " 名管理员不在 MDM 中 (ID: "
                + String.join(", ", adminIds)
                + ")，继续同步将被标记为离职。"
                + "请先在 MDM 中维护管理员记录，或手动设置其 status='C' 后再重试。"
            );
        }

        // 2. 同步流程
        int inserted = userMapper.syncHrInsert();
        int updated = userMapper.syncHrUpdate();
        int resigned = userMapper.syncHrMarkResigned();
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("resigned", resigned);
        return result;
    }
}
