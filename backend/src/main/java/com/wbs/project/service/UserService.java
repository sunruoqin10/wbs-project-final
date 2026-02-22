package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

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

        user.setId("u" + UUID.randomUUID().toString().substring(0, 8));
        user.setJoinedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
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
}
