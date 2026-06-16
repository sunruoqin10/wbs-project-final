package com.wbs.project.service;

import com.wbs.project.entity.RoleChangeLog;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.RoleChangeLogMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService.syncHrData 步 ④ JPSTN_CD 启发式预填推断 单测(2026-06-16)
 * 覆盖 spec §8 E1/E3/E4/E5/E7/E8/E9
 * E14(整事务回滚)不在单测范围,留到 Task 15 集成验收
 */
class UserServiceJpstnInferenceTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleChangeLogMapper roleChangeLogMapper;
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // syncHrInsert / syncHrUpdate / syncHrMarkResigned 默认 0(@Mock 默认),无需显式 mock
    }

    /** E1: BA member → 升级 dept-pm,managed_dept_codes=[dept_code],managed_company_cd=company_cd,写 audit */
    @Test
    void syncHrData_baMember_upgradesToDeptPm() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000001"));
        User u = new User();
        u.setId("C0000001");
        u.setRole("member");
        u.setJpstnCd("BA");
        u.setDeptCode("D001");
        u.setCompanyCd("2700");
        when(userMapper.selectByIds(List.of("C0000001"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(1, result.get("inferred"));
        assertTrue(result.containsKey("inferred"));
        // 断言 updateRoleAndScope 被调用且入参正确
        verify(userMapper).updateRoleAndScope(eq("C0000001"), eq("dept-project-manager"),
                argThat(s -> s != null && s.contains("D001")), eq("2700"), isNull());
        // 断言 audit log 被写入
        ArgumentCaptor<RoleChangeLog> logCaptor = ArgumentCaptor.forClass(RoleChangeLog.class);
        verify(roleChangeLogMapper).insert(logCaptor.capture());
        RoleChangeLog log = logCaptor.getValue();
        assertEquals("C0000001", log.getUserId());
        assertEquals("member", log.getOldRole());
        assertEquals("dept-project-manager", log.getNewRole());
        assertEquals("HR_SYNC", log.getChangedBy());
        assertNotNull(log.getReason());
        assertTrue(log.getReason().contains("BA"));
        assertNotNull(log.getNewManagedDeptCodes());
        assertTrue(log.getNewManagedDeptCodes().contains("D001"));
        assertEquals("2700", log.getNewManagedCompanyCd());
        assertNull(log.getOldManagedDeptCodes());
        assertNull(log.getOldManagedCompanyCd());
        assertNull(log.getNewManagedProjectIds());
    }

    /** E2: BF member → 升级 PM,managed_project_ids='[]',写 audit */
    @Test
    void syncHrData_bfMember_upgradesToProjectManager() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000002"));
        User u = new User();
        u.setId("C0000002");
        u.setRole("member");
        u.setJpstnCd("BF");
        when(userMapper.selectByIds(List.of("C0000002"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(1, result.get("inferred"));
        verify(userMapper).updateRoleAndScope(eq("C0000002"), eq("project-manager"),
                isNull(), isNull(), eq("[]"));
        ArgumentCaptor<RoleChangeLog> logCaptor = ArgumentCaptor.forClass(RoleChangeLog.class);
        verify(roleChangeLogMapper).insert(logCaptor.capture());
        RoleChangeLog log = logCaptor.getValue();
        assertEquals("project-manager", log.getNewRole());
        assertEquals("[]", log.getNewManagedProjectIds());
        assertEquals("HR_SYNC", log.getChangedBy());
    }

    /** E3: jpstnCd 不是 BA/BF → 不动 */
    @Test
    void syncHrData_unknownJpstnCd_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000003"));
        User u = new User();
        u.setId("C0000003");
        u.setRole("member");
        u.setJpstnCd("X1");
        when(userMapper.selectByIds(List.of("C0000003"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** E4: role 已是 admin → 不动(启发式只覆盖 member) */
    @Test
    void syncHrData_alreadyAdmin_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000004"));
        User u = new User();
        u.setId("C0000004");
        u.setRole("admin");
        u.setJpstnCd("BA");
        u.setDeptCode("D001");
        u.setCompanyCd("2700");
        when(userMapper.selectByIds(List.of("C0000004"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** E5: 已是 dept-pm(被手工改过)→ 不动 */
    @Test
    void syncHrData_alreadyDeptPm_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000005"));
        User u = new User();
        u.setId("C0000005");
        u.setRole("dept-project-manager");
        u.setJpstnCd("BF");
        when(userMapper.selectByIds(List.of("C0000005"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** E7: jpstnCd 从 null → BA → 升级(覆盖) */
    @Test
    void syncHrData_nullToBa_upgrades() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000007"));
        User u = new User();
        u.setId("C0000007");
        u.setRole("member");
        u.setJpstnCd("BA");
        u.setDeptCode("D007");
        u.setCompanyCd("8400");
        when(userMapper.selectByIds(List.of("C0000007"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(1, result.get("inferred"));
        verify(userMapper).updateRoleAndScope(eq("C0000007"), eq("dept-project-manager"),
                argThat(s -> s != null && s.contains("D007")), eq("8400"), isNull());
    }

    /** E8: BA 但 dept_code 为空 → 跳过 */
    @Test
    void syncHrData_baButNoDeptCode_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000008"));
        User u = new User();
        u.setId("C0000008");
        u.setRole("member");
        u.setJpstnCd("BA");
        u.setDeptCode(null);
        u.setCompanyCd("2700");
        when(userMapper.selectByIds(List.of("C0000008"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** E9: BA 但 company_cd 为空 → 跳过 */
    @Test
    void syncHrData_baButNoCompanyCd_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000009"));
        User u = new User();
        u.setId("C0000009");
        u.setRole("member");
        u.setJpstnCd("BA");
        u.setDeptCode("D009");
        u.setCompanyCd(null);
        when(userMapper.selectByIds(List.of("C0000009"))).thenReturn(List.of(u));

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** E10/E14-related: selectMdmActiveEmpNums 返空 → inferred=0,不调任何 mapper */
    @Test
    void syncHrData_noActiveEmpNums_skips() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of());

        var result = userService.syncHrData();

        assertEquals(0, result.get("inferred"));
        verify(userMapper, never()).selectByIds(any());
        verify(userMapper, never()).updateRoleAndScope(anyString(), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, never()).insert(any(RoleChangeLog.class));
    }

    /** 1 次 syncHrData 多用户:BA + BF + skip X1 → inferred=2 */
    @Test
    void syncHrData_mixedBatch_correctCount() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of("C0000010", "C0000011", "C0000012"));
        User u1 = new User();
        u1.setId("C0000010");
        u1.setRole("member");
        u1.setJpstnCd("BA");
        u1.setDeptCode("D010");
        u1.setCompanyCd("2700");

        User u2 = new User();
        u2.setId("C0000011");
        u2.setRole("member");
        u2.setJpstnCd("BF");

        User u3 = new User();
        u3.setId("C0000012");
        u3.setRole("member");
        u3.setJpstnCd("X1");

        when(userMapper.selectByIds(List.of("C0000010", "C0000011", "C0000012")))
                .thenReturn(List.of(u1, u2, u3));

        var result = userService.syncHrData();

        assertEquals(2, result.get("inferred"));
        verify(userMapper).updateRoleAndScope(eq("C0000010"), eq("dept-project-manager"),
                anyString(), eq("2700"), isNull());
        verify(userMapper).updateRoleAndScope(eq("C0000011"), eq("project-manager"),
                isNull(), isNull(), eq("[]"));
        verify(userMapper, never()).updateRoleAndScope(eq("C0000012"), anyString(), any(), any(), any());
        verify(roleChangeLogMapper, times(2)).insert(any(RoleChangeLog.class));
    }

    /** syncHrData 返回 Map 必含 4 个 key */
    @Test
    void syncHrData_returnsFourKeys() {
        when(userMapper.selectAdminIdsNotInMdm()).thenReturn(List.of());
        when(userMapper.selectMdmActiveEmpNums()).thenReturn(List.of());

        var result = userService.syncHrData();

        assertTrue(result.containsKey("inserted"));
        assertTrue(result.containsKey("updated"));
        assertTrue(result.containsKey("resigned"));
        assertTrue(result.containsKey("inferred"));
    }
}
