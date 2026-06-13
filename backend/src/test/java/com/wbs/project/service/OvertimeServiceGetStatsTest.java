package com.wbs.project.service;

import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OvertimeService.getStats dept-pm SQL 过滤下沉 单测(2026-06-13)
 * 验证: dept-pm 调用 getStats 时,selectByCondition 收到的 userIds 不为 null
 */
@ExtendWith(MockitoExtension.class)
class OvertimeServiceGetStatsTest {

    @Mock OvertimeMapper overtimeMapper;
    @Mock UserMapper userMapper;
    @Mock ProjectMapper projectMapper;
    @Mock ProjectMemberMapper projectMemberMapper;
    @Mock TaskMapper taskMapper;
    @Mock EmailNotificationService emailNotificationService;
    @Mock UserService userService;
    @Mock PermissionService permissionService;

    @InjectMocks OvertimeService overtimeService;

    @Test
    void deptPm_getStats_passesUserIdsToMapper() {
        when(permissionService.getAccessibleOvertimeUserIds("C0000001"))
                .thenReturn(java.util.Set.of("C0000002", "C0000003"));
        when(overtimeMapper.selectByCondition(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        overtimeService.getStats(null, null, null, null, "C0000001");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> userIdsCap = ArgumentCaptor.forClass(List.class);
        verify(overtimeMapper).selectByCondition(
                any(), any(), any(), any(), any(), any(), userIdsCap.capture());
        assertEquals(java.util.Set.of("C0000002", "C0000003"),
                new java.util.HashSet<>(userIdsCap.getValue()));
    }

    @Test
    void admin_getStats_passesNullUserIdsToMapper() {
        when(permissionService.getAccessibleOvertimeUserIds("C0000099")).thenReturn(null);
        when(overtimeMapper.selectByCondition(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());

        overtimeService.getStats(null, null, null, null, "C0000099");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> userIdsCap = ArgumentCaptor.forClass(List.class);
        verify(overtimeMapper).selectByCondition(
                any(), any(), any(), any(), any(), any(), userIdsCap.capture());
        assertNull(userIdsCap.getValue());
    }
}