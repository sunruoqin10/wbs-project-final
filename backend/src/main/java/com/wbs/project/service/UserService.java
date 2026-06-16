package com.wbs.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbs.project.entity.RoleChangeLog;
import com.wbs.project.entity.User;
import com.wbs.project.enums.UserRole;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.RoleChangeLogMapper;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * 用户Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final RoleChangeLogMapper roleChangeLogMapper;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        return userMapper.selectAll();
    }

    /**
     * 根据ID查询用户
     */
    public User getUserById(String id) {
        return userMapper.selectById(id);
    }

    /**
     * 根据邮箱查询用户
     */
    public User getUserByEmail(String email) {
        return userMapper.selectByEmail(email);
    }

    /**
     * 根据ID列表查询用户
     */
    public List<User> getUsersByIds(List<String> ids) {
        return userMapper.selectByIds(ids);
    }

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        // 验证邮箱唯一性
        User existingUser = userMapper.selectByEmail(user.getEmail());
        if (existingUser != null) {
            throw new RuntimeException("邮箱已被注册");
        }

        user.setId(generateNextUserId());
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            user.setPassword("123456");
        }
        user.setJoinedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        userMapper.insert(user);
        return user;
    }

    /**
     * 生成下一个用户ID（C0000001, C0000002, ... 格式）
     */
    private String generateNextUserId() {
        final String PREFIX = "C";

        String maxId = userMapper.selectMaxIdByPrefix(PREFIX);
        long nextNum = 1L;
        if (maxId != null && maxId.length() > PREFIX.length()) {
            try {
                String numPart = maxId.substring(PREFIX.length());
                nextNum = Long.parseLong(numPart) + 1;
            } catch (NumberFormatException e) {
                nextNum = 1L;
            }
        }
        return String.format("%s%07d", PREFIX, nextNum);
    }

    /**
     * 更新用户
     */
    @Transactional
    public User updateUser(String id, User user) {
        User existingUser = userMapper.selectById(id);
        if (existingUser == null) {
            throw new RuntimeException("用户不存在");
        }

        // 如果修改邮箱，验证新邮箱的唯一性
        /* 
        if (!existingUser.getEmail().equals(user.getEmail())) {
            User emailUser = userMapper.selectByEmail(user.getEmail());
            if (emailUser != null) {
                throw new RuntimeException("邮箱已被使用");
            }
        }
        */

        user.setId(id);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
        return userMapper.selectById(id);
    }

    /**
     * 删除用户
     * 注意：需要级联删除相关数据（在Java代码中处理）
     */
    @Transactional
    public void deleteUser(String id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // TODO: 在这里处理级联删除逻辑
        // 1. 删除用户相关的项目成员关系
        // 2. 删除用户相关的任务分配关系
        // 3. 删除用户相关的评论
        // 4. 删除用户上传的附件

        userMapper.deleteById(id);
    }

    /**
     * 获取用户总数
     */
    public int getTotalUsers() {
        return userMapper.countTotal();
    }

    /**
     * 用户登录（明文密码验证 - 使用用户ID）
     */
    public User login(String userId, String password) {
        // 根据用户ID查询
        User user = userMapper.selectById(userId);

        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 明文密码比对
        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        return user;
    }

    /**
     * 获取当前 token_version（供 JwtUtil.generateToken 使用）
     */
    public long getCurrentTokenVersion(String userId) {
        User u = userMapper.selectById(userId);
        return u == null || u.getTokenVersion() == null ? 0L : u.getTokenVersion().longValue();
    }

    /**
     * 修改密码
     */
    @Transactional
    public void changePassword(String id, String currentPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!currentPassword.equals(user.getPassword())) {
            throw new RuntimeException("当前密码错误");
        }
        user.setPassword(newPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
    }

    /**
     * 获取所有项目经理
     */
    public List<User> getManagers() {
        return userMapper.selectByRole("project-manager");
    }

    /**
     * 关键词搜索用户（支持分页）
     */
    public java.util.Map<String, Object> searchUsers(String keyword, int page, int pageSize) {
        int offset = (page - 1) * pageSize;
        List<User> records = userMapper.searchUsers(keyword, offset, pageSize);
        int total = userMapper.countSearchUsers(keyword);
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        result.put("records", records);
        result.put("total", total);
        return result;
    }

    /**
     * 同步人事数据
     * 从 mdm_if_pa_a 和 mdm_if_or_a 表同步数据到 sys_user
     * 流程：插入新用户 → 更新已有用户 → 标记离职用户
     * 同步前会阻断「在岗 admin 但 MDM 缺失」的情况，避免误标管理员为离职
     * @return 包含 inserted / updated / resigned 数量的 Map
     */
    @Transactional
    public java.util.Map<String, Integer> syncHrData() {
        // 1. 同步前安全检查：阻断 admin 误标
        java.util.List<String> adminIds = userMapper.selectAdminIdsNotInMdm();
        if (!adminIds.isEmpty()) {
            throw new BusinessException(
                409,
                "检测到 " + adminIds.size() + " 名管理员不在 MDM 中 (ID: "
                + String.join(", ", adminIds)
                + ")，继续同步将被标记为离职。"
                + "请先在 MDM 中维护管理员记录，或手动设置其 status='C' 后再重试。"
            );
        }

        // 2. 同步流程
        int inserted = userMapper.syncHrInsert();
        int updated = userMapper.syncHrUpdate();
        int resigned = userMapper.syncHrMarkResigned();
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("resigned", resigned);
        return result;
    }

    // ============ 角色管理 v2 ============

    /**
     * 修改用户角色与管辖范围
     * - 调用方须为 admin（在 Controller 层通过 @RequireRole("admin") 或 PermissionService.requireAdmin 拦截）
     * - dept-project-manager 必须指定 managedCompanyCd == user.companyCd
     * - managedDeptCodes 自动序列化为 JSON
     * - 触发 token_version + 1,旧 JWT 立即失效
     * - 写 sys_role_change_log 审计
     *
     * @param operatorId       操作人 id
     * @param targetUserId     被操作用户 id
     * @param newRole          新角色（UserRole 枚举 code）
     * @param managedDeptCodes 管辖部门编码列表（仅 dept-project-manager 必填）
     * @param managedCompanyCd 管辖公司编码（仅 dept-project-manager 必填）
     * @param reason           变更原因
     * @return 更新后的用户
     */
    @Transactional
    public User changeUserRole(String operatorId,
                                String targetUserId,
                                String newRole,
                                List<String> managedDeptCodes,
                                String managedCompanyCd,
                                List<String> managedProjectIds,
                                String reason) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(404, "用户不存在: " + targetUserId);
        }

        UserRole newRoleEnum = UserRole.fromCode(newRole);

        // 权限校验(2026-06-12):admin 可改任何人;dept-pm 仅可改本部门非 admin 用户
        if (!permissionService.canChangeRoleInDept(operatorId, targetUserId, newRole)) {
            throw new BusinessException(403, "无权修改该用户角色");
        }

        // 校验 dept-project-manager 必须指定 managed_company_cd
        String jsonCodes = null;
        String jsonProjects = null;
        if (newRoleEnum == UserRole.DEPT_PROJECT_MANAGER) {
            if (managedDeptCodes == null || managedDeptCodes.isEmpty()) {
                throw new BusinessException(400, "dept-project-manager 必须指定至少一个管辖部门");
            }
            if (managedCompanyCd == null || managedCompanyCd.isEmpty()) {
                throw new BusinessException(400, "dept-project-manager 必须指定 managed_company_cd");
            }
            if (target.getCompanyCd() != null && !target.getCompanyCd().equals(managedCompanyCd)) {
                throw new BusinessException(400, "managed_company_cd 必须与用户所属公司一致 (user.company_cd=" + target.getCompanyCd() + ")");
            }
            try {
                jsonCodes = objectMapper.writeValueAsString(managedDeptCodes);
            } catch (Exception e) {
                throw new BusinessException(500, "managedDeptCodes 序列化失败: " + e.getMessage());
            }
        } else if (newRoleEnum == UserRole.PROJECT_MANAGER) {
            // 切到 PM(2026-06-12):managedProjectIds 可选(可以稍后单独调 updateManagedProjects)
            if (managedProjectIds != null) {
                try {
                    jsonProjects = objectMapper.writeValueAsString(managedProjectIds);
                } catch (Exception e) {
                    throw new BusinessException(500, "managedProjectIds 序列化失败: " + e.getMessage());
                }
            }
            // 切到 PM 时清空 dept-pm 字段
            managedDeptCodes = null;
            managedCompanyCd = null;
            jsonCodes = null;
        } else {
            // 切到 member / viewer 时清空所有 managed_*
            managedDeptCodes = null;
            managedCompanyCd = null;
            managedProjectIds = null;
            jsonCodes = null;
            jsonProjects = null;
        }

        // 写审计
        RoleChangeLog changeLog = new RoleChangeLog();
        changeLog.setUserId(targetUserId);
        changeLog.setOldRole(target.getRole());
        changeLog.setNewRole(newRoleEnum.code);
        changeLog.setOldManagedDeptCodes(target.getManagedDeptCodes());
        changeLog.setNewManagedDeptCodes(jsonCodes);
        changeLog.setOldManagedCompanyCd(target.getManagedCompanyCd());
        changeLog.setNewManagedCompanyCd(managedCompanyCd);
        changeLog.setChangedBy(operatorId);
        changeLog.setChangedAt(LocalDateTime.now());
        changeLog.setReason(reason);
        roleChangeLogMapper.insert(changeLog);

        // 更新用户(role + managed_* + token_version+1)
        userMapper.updateRoleAndScope(targetUserId, newRoleEnum.code, jsonCodes, managedCompanyCd, jsonProjects);
        log.info("角色变更: operator={}, target={}, {} -> {}, deptCodes={}, companyCd={}, projectIds={}, reason={}",
                operatorId, targetUserId, changeLog.getOldRole(), changeLog.getNewRole(),
                jsonCodes, managedCompanyCd, jsonProjects, reason);

        return userMapper.selectById(targetUserId);
    }

    /**
     * 仅更新 PM 的 managed_project_ids(2026-06-12 新增)
     * 不动 role,只刷项目列表;权限由 Controller 层用 isAdmin/isDeptProjectManager 校验
     */
    @Transactional
    public void updateManagedProjects(String targetUserId, List<String> managedProjectIds) {
        User target = userMapper.selectById(targetUserId);
        if (target == null) {
            throw new BusinessException(404, "用户不存在: " + targetUserId);
        }
        if (!UserRole.PROJECT_MANAGER.code.equals(target.getRole())) {
            throw new BusinessException(400, "只能给 project-manager 角色分配项目");
        }
        String json = null;
        if (managedProjectIds != null) {
            try {
                json = objectMapper.writeValueAsString(managedProjectIds);
            } catch (Exception e) {
                throw new BusinessException(500, "managedProjectIds 序列化失败: " + e.getMessage());
            }
        }
        userMapper.updateManagedProjects(targetUserId, json);
        log.info("PM 项目分配更新: target={}, projectIds={}", targetUserId, json);
    }

    /**
     * 查询某用户的角色变更历史
     */
    public List<RoleChangeLog> getRoleChangeHistory(String userId) {
        return roleChangeLogMapper.selectByUserId(userId);
    }

    /**
     * 解析 managedDeptCodes JSON 字符串为 List（供其他 Service 复用）
     */
    public List<String> parseManagedDeptCodes(String json) {
        if (json == null || json.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("解析 managedDeptCodes 失败: {}", json, e);
            return Collections.emptyList();
        }
    }
}
