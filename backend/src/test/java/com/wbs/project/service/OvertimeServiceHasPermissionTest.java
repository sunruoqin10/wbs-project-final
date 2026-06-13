package com.wbs.project.service;

import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.hasPermission dept-pm 路径单测(2026-06-13)
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceHasPermissionTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    private User deptPm(String id) {
        User u = new User();
        u.setId(id);
        u.setRole("dept-project-manager");
        return u;
    }

    private User member(String id, String deptCode) {
        User u = new User();
        u.setId(id);
        u.setRole("member");
        u.setDeptCode(deptCode);
        return u;
    }

    @Test
    void deptPm_canViewRecordOfManagedDeptMember() {
        // dept-pm C0000001 manages ["D001"]
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);
        // record owner C0000002 dept = D001
        when(userMapper.selectById("C0000002")).thenReturn(member("C0000002", "D001"));
        when(permissionService.isDeptManager("C0000001", "D001")).thenReturn(true);

        assertTrue(overtimeService.hasPermission("C0000001", "P001", "C0000002"));
    }

    @Test
    void deptPm_cannotViewRecordOfOtherDeptMember() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);
        when(userMapper.selectById("C0000003")).thenReturn(member("C0000003", "D999"));
        when(permissionService.isDeptManager("C0000001", "D999")).thenReturn(false);

        // 既不是 owner 也不是 dept 内,落到 project.ownerId 检查
        Project p = new Project();
        p.setId("P001");
        p.setOwnerId("C0000099");
        when(projectMapper.selectById("P001")).thenReturn(p);

        assertFalse(overtimeService.hasPermission("C0000001", "P001", "C0000003"));
    }

    @Test
    void deptPm_canAlwaysViewOwnRecord() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);

        assertTrue(overtimeService.hasPermission("C0000001", "P001", "C0000001"));
    }

    @Test
    void deptPm_recordUserIdNull_fallsThroughToOwnerCheck() {
        User actor = deptPm("C0000001");
        when(userMapper.selectById("C0000001")).thenReturn(actor);

        Project p = new Project();
        p.setId("P001");
        p.setOwnerId("C0000001"); // actor 是 owner
        when(projectMapper.selectById("P001")).thenReturn(p);

        // recordUserId 为 null:不应触发 dept-pm 分支,走 owner 路径放行
        assertTrue(overtimeService.hasPermission("C0000001", "P001", null));
    }
}