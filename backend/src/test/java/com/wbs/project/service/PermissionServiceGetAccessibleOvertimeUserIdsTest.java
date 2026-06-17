package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.PermissionMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * PermissionService.getAccessibleOvertimeUserIds 单测(2026-06-13)
 */
@ExtendWith(MockitoExtension.class)
class PermissionServiceGetAccessibleOvertimeUserIdsTest {

    @Mock PermissionMapper permissionMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock ProjectMapper projectMapper;
    @Mock UserMapper userMapper;
    @Mock TaskMapper taskMapper;

    @InjectMocks PermissionService permissionService;

    private User user(String id, String role) {
        User u = new User();
        u.setId(id);
        u.setRole(role);
        return u;
    }

    @Test
    void admin_returnsNull() {
        when(userMapper.selectById("A1")).thenReturn(user("A1", "admin"));
        assertNull(permissionService.getAccessibleOvertimeUserIds("A1"));
    }

    @Test
    void projectManager_returnsNull() {
        when(userMapper.selectById("PM1")).thenReturn(user("PM1", "project-manager"));
        assertNull(permissionService.getAccessibleOvertimeUserIds("PM1"));
    }

    @Test
    void deptPm_returnsDeptMembers() {
        User u = user("D1", "dept-project-manager");
        u.setManagedDeptCodes("[\"D001\",\"D002\"]");
        when(userMapper.selectById("D1")).thenReturn(u);
        when(userMapper.selectIdsByDeptCodes(List.of("D001", "D002")))
                .thenReturn(List.of("U1", "U2", "U3"));

        Set<String> result = permissionService.getAccessibleOvertimeUserIds("D1");
        assertNotNull(result);
        assertEquals(Set.of("U1", "U2", "U3"), result);
    }

    @Test
    void deptPm_noManagedDepts_returnsEmpty() {
        User u = user("D1", "dept-project-manager");
        u.setManagedDeptCodes(null);
        when(userMapper.selectById("D1")).thenReturn(u);

        assertEquals(Set.of(), permissionService.getAccessibleOvertimeUserIds("D1"));
    }

    @Test
    void member_returnsSelf() {
        when(userMapper.selectById("M1")).thenReturn(user("M1", "member"));
        assertEquals(Set.of("M1"), permissionService.getAccessibleOvertimeUserIds("M1"));
    }
}
