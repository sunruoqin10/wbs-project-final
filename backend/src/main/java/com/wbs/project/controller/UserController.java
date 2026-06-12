package com.wbs.project.controller;

import com.wbs.project.annotation.RequireRole;
import com.wbs.project.common.Result;
import com.wbs.project.dto.ManagedProjectsRequest;
import com.wbs.project.entity.ChangePasswordRequest;
import com.wbs.project.entity.LoginRequest;
import com.wbs.project.entity.LoginResponse;
import com.wbs.project.entity.RoleChangeLog;
import com.wbs.project.entity.RoleChangeRequest;
import com.wbs.project.entity.User;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.service.PermissionService;
import com.wbs.project.service.UserService;
import com.wbs.project.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final PermissionService permissionService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        User user = userService.login(request.getUserId(), request.getPassword());
        user.setPassword(null);
        // 角色管理 v2:token 携带 tokenVersion,角色/管辖范围变更后旧 token 失效
        long tokenVersion = userService.getCurrentTokenVersion(user.getId());
        String token = jwtUtil.generateToken(user.getId(), user.getRole(), tokenVersion);
        LoginResponse loginResponse = new LoginResponse(token, user);
        return Result.success("登录成功", loginResponse);
    }

    @GetMapping
    public Result<?> getAllUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize) {
        if (keyword != null && !keyword.isEmpty()) {
            // 带关键词搜索，返回分页结果
            return Result.success(userService.searchUsers(keyword, page, pageSize));
        }
        // 无关键词时返回全部（兼容旧逻辑）
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

    /**
     * 创建用户（角色管理 v2：非 admin 不能创建 admin 账号）
     */
    @PostMapping
    public Result<User> createUser(@RequestBody User user, HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        if (operatorId == null) {
            return Result.error(401, "未登录");
        }
        if ("admin".equals(user.getRole()) && !permissionService.isAdmin(operatorId)) {
            return Result.error(403, "无权限创建管理员账号");
        }
        if ("dept-project-manager".equals(user.getRole()) && !permissionService.isAdmin(operatorId)) {
            return Result.error(403, "无权限创建部门项目负责人账号");
        }
        User createdUser = userService.createUser(user);
        return Result.success("用户创建成功", createdUser);
    }

    @PutMapping("/{id}")
    public Result<User> updateUser(@PathVariable String id, @RequestBody User user) {
        User updatedUser = userService.updateUser(id, user);
        return Result.success("用户更新成功", updatedUser);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @GetMapping("/count")
    public Result<Integer> getTotalUsers() {
        int count = userService.getTotalUsers();
        return Result.success(count);
    }

    @PutMapping("/{id}/password")
    public Result<String> changePassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
        return Result.success("密码修改成功");
    }

    @PostMapping("/sync-hr")
    public Result<java.util.Map<String, Integer>> syncHrData() {
        java.util.Map<String, Integer> result = userService.syncHrData();
        return Result.success("人事数据同步完成", result);
    }

    // ============ 角色管理 v2 ============

    /**
     * 修改用户角色与管辖范围
     * - admin 可改任何用户(含 admin 互改)
     * - dept-project-manager 仅可改本部门内非 admin 用户(2026-06-12 放开)
     * 触发 tokenVersion + 1,旧 token 立即失效
     */
    @PutMapping("/{id}/role")
    public Result<User> changeUserRole(@PathVariable String id,
                                        @RequestBody RoleChangeRequest req,
                                        HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        User updated = userService.changeUserRole(operatorId, id, req.getNewRole(),
                req.getManagedDeptCodes(), req.getManagedCompanyCd(),
                req.getManagedProjectIds(), req.getReason());
        return Result.success("角色变更成功,目标用户需重新登录", updated);
    }

    /**
     * 仅更新 PM 的 managed_project_ids(项目分配)
     * 2026-06-12 新增,供 dept-pm 单独管理 PM 的项目列表
     * 触发 tokenVersion + 1
     */
    @PutMapping("/{id}/managed-projects")
    public Result<User> updateManagedProjects(@PathVariable String id,
                                              @RequestBody ManagedProjectsRequest req,
                                              HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        // 权限:admin / dept-pm(本部门内)
        if (!permissionService.isAdmin(operatorId) && !permissionService.isDeptProjectManager(operatorId)) {
            throw new BusinessException(403, "无权分配项目");
        }
        User target = userService.getUserById(id);
        if (target == null) {
            throw new BusinessException(404, "用户不存在: " + id);
        }
        // dept-pm 必须本部门内
        if (!permissionService.isAdmin(operatorId)
                && !permissionService.isDeptManager(operatorId, target.getDeptCode())) {
            throw new BusinessException(403, "目标用户不在您管辖的部门内");
        }
        userService.updateManagedProjects(id, req.getManagedProjectIds());
        return Result.success("项目分配成功", userService.getUserById(id));
    }

    /**
     * 查询某用户角色变更历史
     */
    @GetMapping("/{id}/role-history")
    public Result<List<RoleChangeLog>> getRoleChangeHistory(@PathVariable String id,
                                                             HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        // 非 admin 只能查自己的历史
        if (operatorId != null && !permissionService.isAdmin(operatorId) && !operatorId.equals(id)) {
            return Result.error(403, "无权限查看他人角色变更历史");
        }
        return Result.success(userService.getRoleChangeHistory(id));
    }
}
