package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.LoginRequest;
import com.wbs.project.entity.LoginResponse;
import com.wbs.project.entity.User;
import com.wbs.project.service.UserService;
import com.wbs.project.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户Controller
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 用户登录（使用JWT令牌）
     * POST /api/users/login
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUserId(), request.getPassword());
            // 清空密码字段
            user.setPassword(null);

            // 生成JWT令牌
            String token = jwtUtil.generateToken(user.getId(), user.getRole());

            // 构造登录响应
            LoginResponse loginResponse = new LoginResponse(token, user);

            return Result.success("登录成功", loginResponse);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取所有用户
     * GET /api/users
     */
    @GetMapping
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    /**
     * 根据ID获取用户
     * GET /api/users/{id}
     */
    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 批量获取用户
     * POST /api/users/batch
     */
    @PostMapping("/batch")
    public Result<List<User>> getUsersByIds(@RequestBody List<String> ids) {
        List<User> users = userService.getUsersByIds(ids);
        return Result.success(users);
    }

    /**
     * 创建用户
     * POST /api/users
     */
    @PostMapping
    public Result<User> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return Result.success("用户创建成功", createdUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新用户
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable String id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return Result.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除用户
     * DELETE /api/users/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取用户总数
     * GET /api/users/count
     */
    @GetMapping("/count")
    public Result<Integer> getTotalUsers() {
        int count = userService.getTotalUsers();
        return Result.success(count);
    }
}
