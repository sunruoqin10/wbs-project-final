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

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.validateApprover dept-pm 路径单测(2026-06-13)
 * validateApprover 是 private,通过反射调用
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceValidateApproverTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    private void invokeValidateApprover(String approverId, String projectId) throws Exception {
        Method m = OvertimeService.class.getDeclaredMethod("validateApprover", String.class, String.class);
        m.setAccessible(true);
        try {
            m.invoke(overtimeService, approverId, projectId);
        } catch (java.lang.reflect.InvocationTargetException e) {
            // 反射调用会把目标异常包成 InvocationTargetException,这里解包以便 assertThrows 能直接拿到原异常
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            }
            throw e;
        }
    }

    @Test
    void deptPm_canApproveRecordInManagedDeptProject() throws Exception {
        User approver = new User();
        approver.setId("C0000001");
        approver.setRole("dept-project-manager");
        when(userMapper.selectById("C0000001")).thenReturn(approver);
        when(permissionService.isDeptProjectManager("C0000001")).thenReturn(true);

        Project p = new Project();
        p.setId("P001");
        p.setDeptCode("D001");
        when(projectMapper.selectById("P001")).thenReturn(p);
        when(permissionService.isDeptManager("C0000001", "D001")).thenReturn(true);

        // 不应抛
        invokeValidateApprover("C0000001", "P001");
    }

    @Test
    void deptPm_cannotApproveRecordInOtherDeptProject() throws Exception {
        User approver = new User();
        approver.setId("C0000001");
        approver.setRole("dept-project-manager");
        when(userMapper.selectById("C0000001")).thenReturn(approver);
        when(permissionService.isDeptProjectManager("C0000001")).thenReturn(true);

        Project p = new Project();
        p.setId("P001");
        p.setDeptCode("D999");
        when(projectMapper.selectById("P001")).thenReturn(p);
        when(permissionService.isDeptManager("C0000001", "D999")).thenReturn(false);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> invokeValidateApprover("C0000001", "P001"));
        assertTrue(ex.getMessage().contains("您没有权限审批"));
    }
}
