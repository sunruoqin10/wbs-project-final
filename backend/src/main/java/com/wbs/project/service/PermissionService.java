package com.wbs.project.service;

import com.wbs.project.entity.Permission;
import com.wbs.project.mapper.PermissionMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionMapper permissionMapper;
    private final ProjectMemberMapper projectMemberMapper;
    
    private final ConcurrentHashMap<String, Set<String>> rolePermissionCache = new ConcurrentHashMap<>();

    public List<Permission> getAllPermissions() {
        return permissionMapper.selectAll();
    }

    public List<Permission> getPermissionsByRole(String role) {
        return permissionMapper.selectByRole(role);
    }

    public boolean hasPermission(String role, String permissionCode) {
        if (role == null || permissionCode == null) {
            return false;
        }
        
        Set<String> permissions = rolePermissionCache.computeIfAbsent(role, r -> {
            List<String> codes = permissionMapper.selectPermissionCodesByRole(r);
            return new HashSet<>(codes);
        });
        
        return permissions.contains(permissionCode);
    }

    public boolean hasProjectPermission(String userId, String projectId, String permissionCode) {
        if (userId == null || projectId == null || permissionCode == null) {
            return false;
        }
        
        if (isProjectOwner(userId, projectId)) {
            return true;
        }
        
        if (isProjectMember(userId, projectId)) {
            return true;
        }
        
        return false;
    }

    public boolean isProjectOwner(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        return projectMemberMapper.isProjectOwner(projectId, userId);
    }

    public boolean isProjectMember(String userId, String projectId) {
        if (userId == null || projectId == null) {
            return false;
        }
        return projectMemberMapper.isProjectMember(projectId, userId);
    }

    public void refreshCache() {
        rolePermissionCache.clear();
    }

    public void refreshCache(String role) {
        rolePermissionCache.remove(role);
    }
}
