package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Permission;
import com.wbs.project.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping
    public Result<List<Permission>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllPermissions();
        return Result.success(permissions);
    }

    @GetMapping("/role/{role}")
    public Result<List<Permission>> getPermissionsByRole(@PathVariable String role) {
        List<Permission> permissions = permissionService.getPermissionsByRole(role);
        return Result.success(permissions);
    }

    @GetMapping("/check")
    public Result<Map<String, Boolean>> checkPermission(
            @RequestParam String role,
            @RequestParam String permission) {
        boolean hasPermission = permissionService.hasPermission(role, permission);
        return Result.success(Map.of("hasPermission", hasPermission));
    }

    @GetMapping("/check-project")
    public Result<Map<String, Boolean>> checkProjectPermission(
            @RequestParam String userId,
            @RequestParam String projectId,
            @RequestParam String permission) {
        boolean hasPermission = permissionService.hasProjectPermission(userId, projectId, permission);
        return Result.success(Map.of("hasPermission", hasPermission));
    }

    @GetMapping("/is-owner")
    public Result<Map<String, Boolean>> isProjectOwner(
            @RequestParam String userId,
            @RequestParam String projectId) {
        boolean isOwner = permissionService.isProjectOwner(userId, projectId);
        return Result.success(Map.of("isOwner", isOwner));
    }

    @GetMapping("/is-member")
    public Result<Map<String, Boolean>> isProjectMember(
            @RequestParam String userId,
            @RequestParam String projectId) {
        boolean isMember = permissionService.isProjectMember(userId, projectId);
        return Result.success(Map.of("isMember", isMember));
    }
}
