package com.wbs.project.controller;

import com.wbs.project.annotation.RequirePermission;
import com.wbs.project.common.Result;
import com.wbs.project.entity.LoginRequest;
import com.wbs.project.entity.LoginResponse;
import com.wbs.project.entity.User;
import com.wbs.project.service.UserService;
import com.wbs.project.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.login(request.getUserId(), request.getPassword());
            user.setPassword(null);
            String token = jwtUtil.generateToken(user.getId(), user.getRole());
            LoginResponse loginResponse = new LoginResponse(token, user);
            return Result.success("登录成功", loginResponse);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping
    @RequirePermission("user:view")
    public Result<List<User>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        return Result.success(users);
    }

    @GetMapping("/{id}")
    public Result<User> getUserById(@PathVariable String id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.error("用户不存在");
        }
        return Result.success(user);
    }

    @PostMapping("/batch")
    public Result<List<User>> getUsersByIds(@RequestBody List<String> ids) {
        List<User> users = userService.getUsersByIds(ids);
        return Result.success(users);
    }

    @PostMapping
    @RequirePermission("user:create")
    public Result<User> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.createUser(user);
            return Result.success("用户创建成功", createdUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @RequirePermission("user:edit")
    public Result<User> updateUser(@PathVariable String id, @RequestBody User user) {
        try {
            User updatedUser = userService.updateUser(id, user);
            return Result.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @RequirePermission("user:delete")
    public Result<Void> deleteUser(@PathVariable String id) {
        try {
            userService.deleteUser(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @GetMapping("/count")
    public Result<Integer> getTotalUsers() {
        int count = userService.getTotalUsers();
        return Result.success(count);
    }
}
