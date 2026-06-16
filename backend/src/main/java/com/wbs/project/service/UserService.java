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
import com.wbs.project.util.JpstnRoleMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        List<User> list = userMapper.selectAll();
        try {
            fillRoleInferredMarkers(list);
        } catch (Exception e) {
            log.warn("填充角色推断标记失败(不影响用户列表返回): {}", e.getMessage());
        }
        return list;
    }

    /**
     * 根据ID查询用户
     */
    public User getUserById(String id) {
        User u = userMapper.selectById(id);
        if (u != null) fillRoleInferredMarkers(Collections.singletonList(u));
        return u;
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
        List<User> list = userMapper.selectByIds(ids);
        try {
            fillRoleInferredMarkers(list);
        } catch (Exception e) {
            log.warn("填充角色推断标记失败(批量查询): {}", e.getMessage());
        }
        return list;
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
        try {
            fillRoleInferredMarkers(records);   // ★ 在 records 装入 Map 之前
        } catch (Exception e) {
            log.warn("填充角色推断标记失败(搜索): {}", e.getMessage());
        }
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
        // ④ ★ HR 同步后按 JPSTN_CD 推断角色(启发式预填,2026-06-16)
        int inferred = inferRoleFromJpstnForHrSync();
        int resigned = userMapper.syncHrMarkResigned();
        java.util.Map<String, Integer> result = new java.util.HashMap<>();
        result.put("inserted", inserted);
        result.put("updated", updated);
        result.put("resigned", resigned);
        result.put("inferred", inferred);
        return result;
    }

    /**
     * 对"mdm 中 C/H 在职"的全体用户跑 JPSTN_CD → role 推断(2026-06-16)
     * 仅当 sys_user.role == 'member' 且 jpstnCd ∈ {BA, BF} 时升级 role + managed_* + 写 audit
     *
     * 事务语义:由外层 syncHrData 的 @Transactional 整事务包裹
     * - 单条推断失败 → 异常向上传播 → 整事务回滚(连同 syncHrInsert / syncHrUpdate),HR 重跑即可
     * - 这里不再写 try/catch:写了也无效,事务回滚照样发生,反而误导
     */
    private int inferRoleFromJpstnForHrSync() {
        List<String> empNums = userMapper.selectMdmActiveEmpNums();
        if (empNums == null || empNums.isEmpty()) return 0;

        List<User> users = userMapper.selectByIds(empNums);
        int inferred = 0;
        for (User u : users) {
            // 二次过滤保护:selectMdmActiveEmpNums 已 WHERE ACT_CLSS_CD IN ('C','H'),
            // 但 sys_user.status 在 syncHrMarkResigned(步 ⑤)之前可能与 mdm 不一致;
            // selectByIds 还 AND status != 'T',实测几乎不会触发 null,
            // 保留此 continue 作为 defensive coding 防 NPE,避免循环中某条 mapper 抛异常导致整循环崩
            if (u == null) continue;

            // 启发式预填:仅当 role 仍为默认 member 时才覆盖
            if (!"member".equals(u.getRole())) continue;
            String newRole = JpstnRoleMapping.inferRoleCode(u.getJpstnCd());
            if (newRole == null) continue;
            UserRole newRoleEnum = UserRole.fromCode(newRole);   // 与 changeUserRole 风格一致,后续用 enum == 比较

            String oldRole = u.getRole();
            // role=member 时 managed_* 无业务意义,audit 里 old_* 强制 null,避免"member 拥有 dept 管理权"的语义混乱
            String oldJsonCodes  = null;
            String oldCompanyCd  = null;
            String oldJsonProj   = null;
            String jsonCodes     = null;
            String companyCd     = null;
            String jsonProjects  = null;

            if (newRoleEnum == UserRole.DEPT_PROJECT_MANAGER) {
                // BA → dept-pm:managed_dept_codes = [user.dept_code], managed_company_cd = user.company_cd
                if (u.getDeptCode() == null || u.getDeptCode().isEmpty()) {
                    log.warn("HR 推断跳过: user={} 职级=BA 但 dept_code 为空", u.getId());
                    continue;
                }
                if (u.getCompanyCd() == null || u.getCompanyCd().isEmpty()) {
                    log.warn("HR 推断跳过: user={} 职级=BA 但 company_cd 为空", u.getId());
                    continue;
                }
                // 序列化风格与 changeUserRole 一致(objectMapper.writeValueAsString),
                // 而不是手拼 JSON,避免引号 / 转义错
                try {
                    jsonCodes = objectMapper.writeValueAsString(Collections.singletonList(u.getDeptCode()));
                } catch (Exception e) {
                    throw new BusinessException(500, "managedDeptCodes 序列化失败: " + e.getMessage());
                }
                companyCd = u.getCompanyCd();
            } else if (newRoleEnum == UserRole.PROJECT_MANAGER) {
                // BF → pm:managed_project_ids = '[]'
                jsonProjects = "[]";
            }

            // 写审计(reuse RoleChangeLog,与 changeUserRole 同口径)
            RoleChangeLog changeLog = new RoleChangeLog();
            changeLog.setUserId(u.getId());
            changeLog.setOldRole(oldRole);
            changeLog.setNewRole(newRole);
            changeLog.setOldManagedDeptCodes(oldJsonCodes);
            changeLog.setNewManagedDeptCodes(jsonCodes);
            changeLog.setOldManagedCompanyCd(oldCompanyCd);
            changeLog.setNewManagedCompanyCd(companyCd);
            changeLog.setOldManagedProjectIds(oldJsonProj);
            changeLog.setNewManagedProjectIds(jsonProjects);
            changeLog.setChangedBy("HR_SYNC");          // ★ 标记来源(手工切 PM 时是 operatorId,不冲突)
            changeLog.setChangedAt(LocalDateTime.now());
            changeLog.setReason("自动推断:" + JpstnRoleMapping.describe(u.getJpstnCd()));
            roleChangeLogMapper.insert(changeLog);

            // 复用现有 mapper(token_version +1 自动包含)
            userMapper.updateRoleAndScope(u.getId(), newRole, jsonCodes, companyCd, jsonProjects);
            log.info("HR 推断: user={} jpstnCd={} {} → {}", u.getId(), u.getJpstnCd(), oldRole, newRole);
            inferred++;
        }
        return inferred;
    }

    /**
     * 给一组 User 填充 roleAutoInferred / roleInferredFromJpstn(2026-06-16 新增)
     * 来源: sys_role_change_log 中 changed_by='HR_SYNC' 的最近一条记录
     * 性能: 分批查询,每批 500 个 ID,避免单次 IN 子句参数过多导致 JDBC/MySQL 报错
     */
    private void fillRoleInferredMarkers(List<User> users) {
        if (users == null || users.isEmpty()) return;
        List<String> ids = users.stream().map(User::getId).collect(Collectors.toList());

        Map<String, String> inferredMap = new HashMap<>();
        int batchSize = 500;
        for (int i = 0; i < ids.size(); i += batchSize) {
            int end = Math.min(i + batchSize, ids.size());
            List<String> batch = ids.subList(i, end);
            List<Map<String, Object>> rows = userMapper.selectLatestHrSyncInferences(batch);
            rows.stream()
                .filter(r -> r.get("user_id") != null && r.get("jpstn_cd") != null)
                .forEach(r -> inferredMap.put((String) r.get("user_id"), (String) r.get("jpstn_cd")));
        }

        for (User u : users) {
            String jpstn = inferredMap.get(u.getId());
            if (jpstn != null) {
                u.setRoleAutoInferred(true);
                u.setRoleInferredFromJpstn(jpstn);
            }
        }
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
        changeLog.setOldManagedProjectIds(target.getManagedProjectIds());
        changeLog.setNewManagedProjectIds(jsonProjects);
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
