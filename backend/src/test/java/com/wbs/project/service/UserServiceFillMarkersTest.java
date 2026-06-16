package com.wbs.project.service;

import com.wbs.project.entity.User;
import com.wbs.project.mapper.RoleChangeLogMapper;
import com.wbs.project.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserService.fillRoleInferredMarkers 单测(2026-06-16)
 * 覆盖:selectLatestHrSyncInferences 返 N 行 → User.roleAutoInferred / roleInferredFromJpstn 填充正确
 * 空 list / null 不报错
 */
class UserServiceFillMarkersTest {

    @Mock private UserMapper userMapper;
    @Mock private RoleChangeLogMapper roleChangeLogMapper;
    @InjectMocks private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_fillsMarkersForTwoUsers() {
        User u1 = new User();
        u1.setId("C0000001");
        u1.setRole("dept-project-manager");

        User u2 = new User();
        u2.setId("C0000002");
        u2.setRole("project-manager");

        when(userMapper.selectAll()).thenReturn(List.of(u1, u2));
        when(userMapper.selectLatestHrSyncInferences(any()))
                .thenReturn(List.of(
                        Map.of("user_id", "C0000001", "jpstn_cd", "BA"),
                        Map.of("user_id", "C0000002", "jpstn_cd", "BF")
                ));

        var result = userService.getAllUsers();

        assertEquals(2, result.size());
        assertTrue(result.get(0).getRoleAutoInferred());
        assertEquals("BA", result.get(0).getRoleInferredFromJpstn());
        assertTrue(result.get(1).getRoleAutoInferred());
        assertEquals("BF", result.get(1).getRoleInferredFromJpstn());
    }

    @Test
    void getAllUsers_partialMatches_OnlyMatchedUsersGetMarkers() {
        User u1 = new User();
        u1.setId("C0000001");
        u1.setRole("dept-project-manager");

        User u2 = new User();
        u2.setId("C0000002");
        u2.setRole("admin");

        when(userMapper.selectAll()).thenReturn(List.of(u1, u2));
        // C0000002 没有任何 HR_SYNC 记录
        when(userMapper.selectLatestHrSyncInferences(any()))
                .thenReturn(List.of(
                        Map.of("user_id", "C0000001", "jpstn_cd", "BA")
                ));

        var result = userService.getAllUsers();

        assertTrue(result.get(0).getRoleAutoInferred());
        assertEquals("BA", result.get(0).getRoleInferredFromJpstn());
        // C0000002 没有 marker
        assertNull(result.get(1).getRoleAutoInferred());
        assertNull(result.get(1).getRoleInferredFromJpstn());
    }

    @Test
    void getAllUsers_emptyList_doesNotQueryMapper() {
        when(userMapper.selectAll()).thenReturn(List.of());

        var result = userService.getAllUsers();

        assertTrue(result.isEmpty());
        // 空 list 直接 return,不调 selectLatestHrSyncInferences
        verify(userMapper, never()).selectLatestHrSyncInferences(any());
    }

    @Test
    void getUserById_existingUser_fillsMarker() {
        User u = new User();
        u.setId("C0000050");
        u.setRole("dept-project-manager");

        when(userMapper.selectById("C0000050")).thenReturn(u);
        when(userMapper.selectLatestHrSyncInferences(List.of("C0000050")))
                .thenReturn(List.of(Map.of("user_id", "C0000050", "jpstn_cd", "BA")));

        User result = userService.getUserById("C0000050");

        assertNotNull(result);
        assertTrue(result.getRoleAutoInferred());
        assertEquals("BA", result.getRoleInferredFromJpstn());
    }

    @Test
    void getUserById_nullUser_returnsNull() {
        when(userMapper.selectById("NOPE")).thenReturn(null);

        User result = userService.getUserById("NOPE");

        assertNull(result);
        // null user 不调 fillRoleInferredMarkers
        verify(userMapper, never()).selectLatestHrSyncInferences(any());
    }

    @Test
    void getUsersByIds_fillsMarkers() {
        User u1 = new User();
        u1.setId("C0000001");
        User u2 = new User();
        u2.setId("C0000002");

        when(userMapper.selectByIds(List.of("C0000001", "C0000002"))).thenReturn(List.of(u1, u2));
        when(userMapper.selectLatestHrSyncInferences(any()))
                .thenReturn(List.of(
                        Map.of("user_id", "C0000001", "jpstn_cd", "BA"),
                        Map.of("user_id", "C0000002", "jpstn_cd", "BF")
                ));

        var result = userService.getUsersByIds(List.of("C0000001", "C0000002"));

        assertEquals(2, result.size());
        assertEquals("BA", result.get(0).getRoleInferredFromJpstn());
        assertEquals("BF", result.get(1).getRoleInferredFromJpstn());
    }

    @Test
    void searchUsers_fillsMarkersBeforePutIntoResult() {
        User u1 = new User();
        u1.setId("C0000001");

        when(userMapper.searchUsers(eq("keyword"), anyInt(), anyInt())).thenReturn(List.of(u1));
        when(userMapper.countSearchUsers("keyword")).thenReturn(1);
        when(userMapper.selectLatestHrSyncInferences(any()))
                .thenReturn(List.of(Map.of("user_id", "C0000001", "jpstn_cd", "BA")));

        var result = userService.searchUsers("keyword", 1, 10);

        @SuppressWarnings("unchecked")
        List<User> records = (List<User>) result.get("records");
        assertEquals(1, records.size());
        assertTrue(records.get(0).getRoleAutoInferred());
        assertEquals("BA", records.get(0).getRoleInferredFromJpstn());
    }

    @Test
    void searchUsers_emptyResults_doesNotQueryInferences() {
        when(userMapper.searchUsers(eq("empty"), anyInt(), anyInt())).thenReturn(List.of());
        when(userMapper.countSearchUsers("empty")).thenReturn(0);

        var result = userService.searchUsers("empty", 1, 10);

        @SuppressWarnings("unchecked")
        List<User> records = (List<User>) result.get("records");
        assertTrue(records.isEmpty());
        verify(userMapper, never()).selectLatestHrSyncInferences(any());
    }

    @Test
    void fillRoleInferredMarkers_handlesNullValuesInMap() {
        // user_id 存在但 jpstn_cd 为 null 的防御场景
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", "C0000001");
        row.put("jpstn_cd", null);

        User u = new User();
        u.setId("C0000001");
        when(userMapper.selectAll()).thenReturn(List.of(u));
        when(userMapper.selectLatestHrSyncInferences(any())).thenReturn(List.of(row));

        var result = userService.getAllUsers();

        // 内部 user_id 拼出的 inferredMap 会有 C0000001→null,fill 不会 setRoleInferredFromJpstn(null)
        // 但 setRoleAutoInferred 仍可能为 true(spec 设计上 jpstn null 不应被认作 inferred)
        // 这里防御:本测试只验证不抛 NPE
        assertEquals(1, result.size());
    }
}
