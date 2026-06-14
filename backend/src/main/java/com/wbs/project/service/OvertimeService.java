package com.wbs.project.service;

import com.wbs.project.dto.OvertimeDTO;
import com.wbs.project.entity.OvertimeApprovalLog;
import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeApprovalLogMapper;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.ProjectMemberMapper;
import com.wbs.project.mapper.TaskMapper;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 加班记录Service
 */
@Service
@RequiredArgsConstructor
public class OvertimeService {

    private final OvertimeMapper overtimeMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final TaskMapper taskMapper;
    private final EmailNotificationService emailNotificationService;
    private final UserService userService;
    private final PermissionService permissionService;
    // 2026-06-14: 加班审批日志 mapper(多角色都可审批,审计追溯)
    private final OvertimeApprovalLogMapper approvalLogMapper;

    // ==================== 权限验证 ====================

    /**
     * 验证用户是否有权限访问指定项目的加班记录
     * 用户有权限的情况：
     * 1. 管理员可以访问所有项目的加班记录
     * 2. 项目经理可以访问所有项目的加班记录
     * 3. 项目负责人（owner_id）可以访问其负责项目的加班记录
     * 4. 用户可以访问自己提交的加班记录
     * @param userId 当前用户ID
     * @param projectId 项目ID
     * @param recordUserId 加班记录的提交者ID（可选）
     * @return 是否有权限
     */
    public boolean hasPermission(String userId, String projectId, String recordUserId) {
        if (userId == null || projectId == null) {
            return false;
        }

        // 查询用户角色
        User user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }

        // 管理员和项目经理可以访问所有项目
        if ("admin".equals(user.getRole()) || "project-manager".equals(user.getRole())) {
            return true;
        }

        // 用户可以访问自己提交的加班记录
        if (recordUserId != null && userId.equals(recordUserId)) {
            return true;
        }

        // 部门项目负责人(2026-06-13): 提交者 dept 在 managed_dept_codes 内则放行
        // 注意: recordUserId 必须非空(避免只看 projectId 误放行),且复用 permissionService.isDeptManager
        // 性能 trade-off: 列表场景下每次调用会多查一次 user(拿 deptCode); 当前 N+1 接受,见 spec §4.1
        if (recordUserId != null) {
            User recordUser = userMapper.selectById(recordUserId);
            if (recordUser != null && recordUser.getDeptCode() != null
                    && permissionService.isDeptManager(userId, recordUser.getDeptCode())) {
                return true;
            }
        }

        // 检查用户是否是项目的负责人（owner_id）
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            return false;
        }

        // 项目负责人可以访问其负责项目的加班记录
        return userId.equals(project.getOwnerId());
    }

    /**
     * 验证用户是否有权限访问指定项目的加班记录（简化版本，不检查记录提交者）
     * @param userId 当前用户ID
     * @param projectId 项目ID
     * @return 是否有权限
     */
    public boolean hasPermission(String userId, String projectId) {
        return hasPermission(userId, projectId, null);
    }

    // ==================== CRUD 操作 ====================

    /**
     * 查询所有加班记录
     */
    public List<OvertimeRecord> getAllRecords() {
        return overtimeMapper.selectAll();
    }

    /**
     * 根据ID查询加班记录
     */
    public OvertimeRecord getRecordById(String id) {
        return overtimeMapper.selectById(id);
    }

    /**
     * 根据用户ID查询加班记录
     */
    public List<OvertimeRecord> getRecordsByUserId(String userId) {
        return overtimeMapper.selectByUserId(userId);
    }

    /**
     * 根据项目ID查询加班记录
     */
    public List<OvertimeRecord> getRecordsByProjectId(String projectId) {
        return overtimeMapper.selectByProjectId(projectId);
    }

    /**
     * 根据状态查询加班记录
     */
    public List<OvertimeRecord> getRecordsByStatus(String status) {
        return overtimeMapper.selectByStatus(status);
    }

    /**
     * 根据条件查询加班记录
     */
    public List<OvertimeRecord> getRecordsByCondition(String userId, String projectId, String status,
                                                       LocalDate startDate, LocalDate endDate, String overtimeType) {
        return overtimeMapper.selectByCondition(userId, projectId, status, startDate, endDate, overtimeType, null);
    }

    /**
     * 创建加班记录
     */
    @Transactional
    public OvertimeRecord createRecord(OvertimeDTO.CreateRequest request) {
        // 验证外键
        validateForeignKeys(request.getUserId(), request.getProjectId(), request.getTaskId());

        // 2026-06-13: 关联任务必须由当前用户负责(所有角色统一)
        if (!permissionService.canCreateOvertimeOnTask(request.getUserId(), request.getTaskId())) {
            throw new RuntimeException("只能为自己负责的任务记加班");
        }

        // 计算加班时长
        BigDecimal hours = calculateHours(request.getStartTime(), request.getEndTime());

        OvertimeRecord record = new OvertimeRecord();
        record.setId("ot" + UUID.randomUUID().toString().substring(0, 8));
        record.setUserId(request.getUserId());
        record.setProjectId(request.getProjectId());
        record.setTaskId(request.getTaskId());
        record.setOvertimeDate(request.getOvertimeDate());
        record.setStartTime(request.getStartTime());
        record.setEndTime(request.getEndTime());
        record.setHours(hours);
        record.setOvertimeType(request.getOvertimeType());
        record.setReason(request.getReason());
        record.setStatus("pending"); // 默认待审批
        record.setCompensationType(request.getCompensationType());
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());

        overtimeMapper.insert(record);
        
        // 发送加班申请提交通知
        OvertimeRecord createdRecord = overtimeMapper.selectById(record.getId());
        User applicant = userService.getUserById(request.getUserId());
        Project project = projectMapper.selectById(request.getProjectId());
        User projectOwner = project != null && project.getOwnerId() != null ? 
            userService.getUserById(project.getOwnerId()) : null;
        
        if (projectOwner != null) {
            emailNotificationService.notifyOvertimeSubmitted(createdRecord, applicant, projectOwner);
        }
        
        return createdRecord;
    }

    /**
     * 更新加班记录
     */
    @Transactional
    public OvertimeRecord updateRecord(String id, OvertimeDTO.UpdateRequest request) {
        OvertimeRecord existing = overtimeMapper.selectById(id);
        if (existing == null) {
            throw new RuntimeException("加班记录不存在");
        }

        // 只有待审批或已拒绝状态的记录可以修改
        if (!"pending".equals(existing.getStatus()) && !"rejected".equals(existing.getStatus())) {
            throw new RuntimeException("只有待审批或已拒绝状态的加班记录可以修改");
        }

        // 验证外键
        validateForeignKeys(existing.getUserId(), request.getProjectId(), request.getTaskId());

        // 2026-06-13: 关联任务必须由当前用户负责(所有角色统一)
        if (!permissionService.canCreateOvertimeOnTask(existing.getUserId(), request.getTaskId())) {
            throw new RuntimeException("只能为自己负责的任务记加班");
        }

        // 计算加班时长
        BigDecimal hours = calculateHours(request.getStartTime(), request.getEndTime());

        existing.setProjectId(request.getProjectId());
        existing.setTaskId(request.getTaskId());
        existing.setOvertimeDate(request.getOvertimeDate());
        existing.setStartTime(request.getStartTime());
        existing.setEndTime(request.getEndTime());
        existing.setHours(hours);
        existing.setOvertimeType(request.getOvertimeType());
        existing.setReason(request.getReason());
        existing.setCompensationType(request.getCompensationType());
        existing.setUpdatedAt(LocalDateTime.now());
        
        // 如果是已拒绝状态修改后重新提交，重置为待审批状态
        if ("rejected".equals(existing.getStatus())) {
            existing.setStatus("pending");
            existing.setApproverId(null);
            existing.setApprovedAt(null);
            existing.setRejectReason(null);
        }

        overtimeMapper.update(existing);
        return overtimeMapper.selectById(id);
    }

    /**
     * 删除加班记录
     */
    @Transactional
    public void deleteRecord(String id) {
        OvertimeRecord record = overtimeMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("加班记录不存在");
        }

        // 只有待审批或已拒绝状态的记录可以删除
        if (!"pending".equals(record.getStatus()) && !"rejected".equals(record.getStatus())) {
            throw new RuntimeException("只有待审批或已拒绝状态的加班记录可以删除");
        }

        overtimeMapper.deleteById(id);
    }

    // ==================== 审批逻辑 ====================

    /**
     * 审批加班申请
     */
    @Transactional
    public OvertimeRecord approveRecord(String id, OvertimeDTO.ApproveRequest request) {
        OvertimeRecord record = overtimeMapper.selectById(id);
        if (record == null) {
            throw new RuntimeException("加班记录不存在");
        }

        // 只有待审批状态可以审批
        if (!"pending".equals(record.getStatus())) {
            throw new RuntimeException("该加班记录已经审批过了");
        }

        // 验证审批人是否有权限（项目负责人可以审批）
        validateApprover(request.getApproverId(), record.getProjectId());

        LocalDateTime now = LocalDateTime.now();

        if (request.getApproved()) {
            // 批准
            record.setStatus("approved");
            record.setApproverId(request.getApproverId());
            record.setApprovedAt(now);
        } else {
            // 拒绝
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new RuntimeException("拒绝时必须填写拒绝原因");
            }
            record.setStatus("rejected");
            record.setApproverId(request.getApproverId());
            record.setApprovedAt(now);
            record.setRejectReason(request.getRejectReason());
        }

        record.setUpdatedAt(now);
        overtimeMapper.update(record);

        // 2026-06-14: 写入审批日志(多角色都可审批,审计追溯)
        // try/catch 兜底:日志写失败不应阻断审批主流程(审批已落库 + 邮件通知)
        try {
            OvertimeApprovalLog log = new OvertimeApprovalLog();
            log.setId("oal" + UUID.randomUUID().toString().substring(0, 8));
            log.setOvertimeId(id);
            log.setApproverId(request.getApproverId());
            log.setApproverRole(resolveApproverRole(request.getApproverId(), record.getProjectId()));
            log.setAction(Boolean.TRUE.equals(request.getApproved()) ? "approve" : "reject");
            if (!Boolean.TRUE.equals(request.getApproved())) {
                log.setRejectReason(request.getRejectReason());
            }
            log.setApprovedAt(now);
            approvalLogMapper.insert(log);
        } catch (Exception e) {
            // 仅记录,不抛(审批主结果已落库)
            org.slf4j.LoggerFactory.getLogger(OvertimeService.class)
                    .error("写入加班审批日志失败 overtimeId={} approverId={}",
                            id, request.getApproverId(), e);
        }

        OvertimeRecord updatedRecord = overtimeMapper.selectById(id);

        // 发送审批结果通知
        User applicant = userService.getUserById(record.getUserId());
        if (request.getApproved()) {
            emailNotificationService.notifyOvertimeApproved(updatedRecord, applicant);
        } else {
            emailNotificationService.notifyOvertimeRejected(updatedRecord, applicant, request.getRejectReason());
        }

        return updatedRecord;
    }

    /**
     * 2026-06-14: 计算审批人当时的角色(快照)
     * - admin / project-manager: 直接返回 user.role
     * - 否则查 project.ownerId: 匹配 → 'project-owner'
     * - 兜底 'dept-project-manager'(validateApprover 已经放行)
     */
    private String resolveApproverRole(String approverId, String projectId) {
        User approver = userMapper.selectById(approverId);
        if (approver == null) return "unknown";
        String role = approver.getRole();
        if ("admin".equals(role) || "project-manager".equals(role)) {
            return role;
        }
        Project project = projectMapper.selectById(projectId);
        if (project != null && approverId.equals(project.getOwnerId())) {
            return "project-owner";
        }
        return "dept-project-manager";
    }

    /**
     * 2026-06-14: 查询某条加班记录的所有审批日志
     * 老记录兜底:日志表为空且主表有 approver_id 时,合成一条虚拟日志条目
     */
    public List<OvertimeApprovalLog> getApprovalLogs(String overtimeId) {
        List<OvertimeApprovalLog> logs = approvalLogMapper.selectByOvertimeId(overtimeId);
        if (logs != null && !logs.isEmpty()) {
            return logs;
        }
        // 老记录兜底:从主表合成虚拟日志条目
        OvertimeRecord record = overtimeMapper.selectById(overtimeId);
        if (record == null || record.getApproverId() == null) {
            return java.util.Collections.emptyList();
        }
        OvertimeApprovalLog fallback = new OvertimeApprovalLog();
        fallback.setId("legacy-" + overtimeId);
        fallback.setOvertimeId(overtimeId);
        fallback.setApproverId(record.getApproverId());
        fallback.setApproverRole(resolveApproverRole(record.getApproverId(), record.getProjectId()));
        fallback.setAction("approved".equals(record.getStatus()) ? "approve" : "reject");
        fallback.setRejectReason(record.getRejectReason());
        fallback.setApprovedAt(record.getApprovedAt());
        return java.util.Collections.singletonList(fallback);
    }

    /**
     * 验证审批人权限
     */
    private void validateApprover(String approverId, String projectId) {
        // 查询审批人
        User approver = userMapper.selectById(approverId);
        if (approver == null) {
            throw new RuntimeException("审批人不存在");
        }

        // 管理员和项目经理可以审批所有项目的加班申请
        if ("admin".equals(approver.getRole()) || "project-manager".equals(approver.getRole())) {
            return;
        }

        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 项目负责人可以审批其负责项目的加班申请
        if (approverId.equals(project.getOwnerId())) {
            return;
        }

        // 部门项目负责人(2026-06-13): 项目归属部门在 managed_dept_codes 内则可审批
        // 判定口径: 按 project.deptCode(而非 recordUser.deptCode),避免提交者换部门后授权漂移
        if (permissionService.isDeptProjectManager(approverId)
                && project.getDeptCode() != null
                && permissionService.isDeptManager(approverId, project.getDeptCode())) {
            return;
        }

        throw new RuntimeException("您没有权限审批此加班申请");
    }

    // ==================== 统计计算 ====================

    /**
     * 获取加班统计信息
     *
     * 2026-06-13: 新签名 + 入口下沉 SQL userIds 过滤。
     * admin / project-manager: accessibleUserIds = null(不限)
     * dept-pm / member / viewer: accessibleUserIds = 可访问子集,SQL 层 IN 守卫
     * 当前 currentUserId 对应的可访问集为空,或限定 userId 不在可访问集内 → 直接返回空 stats
     *
     * ⚠️ 计划性 BREAKING: 本方法新增第 5 参 currentUserId,OvertimeController 调用方
     * 仍是 4 参旧签名,TODO(Task 7)统一改造 controller 透传 currentUserId。
     */
    public OvertimeDTO.OvertimeStats getStats(String userId, String projectId,
                                              LocalDate startDate, LocalDate endDate,
                                              String currentUserId) {
        // 解析当前用户的可访问 userId 集合
        List<String> accessibleUserIds = null; // null = 不限
        if (currentUserId != null) {
            java.util.Set<String> set = permissionService.getAccessibleOvertimeUserIds(currentUserId);
            if (set == null) {
                accessibleUserIds = null; // admin/PM: 不限
            } else if (set.isEmpty()) {
                // 当前用户无任何可访问加班源,直接返回空 stats
                return buildEmptyStats();
            } else {
                accessibleUserIds = new java.util.ArrayList<>(set);
                // 调用方 userId 与 accessibleUserIds 做 AND 关系
                if (userId != null && !accessibleUserIds.contains(userId)) {
                    // 调用方限定到具体人,但该人不属于当前用户可访问范围 → 空 stats
                    return buildEmptyStats();
                }
            }
        }

        List<OvertimeRecord> records = overtimeMapper.selectByCondition(
                userId, projectId, null, startDate, endDate, null, accessibleUserIds);

        OvertimeDTO.OvertimeStats stats = new OvertimeDTO.OvertimeStats();
        stats.setTotalRecords(records.size());
        stats.setPendingRecords((int) records.stream().filter(r -> "pending".equals(r.getStatus())).count());
        stats.setApprovedRecords((int) records.stream().filter(r -> "approved".equals(r.getStatus())).count());
        stats.setRejectedRecords((int) records.stream().filter(r -> "rejected".equals(r.getStatus())).count());

        stats.setTotalHours(records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        stats.setPendingHours(records.stream()
                .filter(r -> "pending".equals(r.getStatus()))
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        stats.setApprovedHours(records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        // 加班总人数（去重）
        stats.setTotalPeople((int) records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .map(OvertimeRecord::getUserId)
                .distinct()
                .count());

        // 待审批数量
        stats.setPendingApprovals((int) records.stream().filter(r -> "pending".equals(r.getStatus())).count());

        // 本月统计
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        List<OvertimeRecord> monthRecords = records.stream()
                .filter(r -> {
                    LocalDate recordDate = r.getOvertimeDate();
                    return recordDate != null && !recordDate.isBefore(monthStart) && !recordDate.isAfter(now);
                })
                .collect(Collectors.toList());

        stats.setThisMonthHours(monthRecords.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .map(OvertimeRecord::getHours)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        stats.setThisMonthPeople((int) monthRecords.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .map(OvertimeRecord::getUserId)
                .distinct()
                .count());

        // 按类型统计
        OvertimeDTO.ByTypeStats byType = new OvertimeDTO.ByTypeStats();
        byType.setWeekday((int) records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .filter(r -> "weekday".equals(r.getOvertimeType())).count());
        byType.setWeekend((int) records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .filter(r -> "weekend".equals(r.getOvertimeType())).count());
        byType.setHoliday((int) records.stream()
                .filter(r -> "approved".equals(r.getStatus()))
                .filter(r -> "holiday".equals(r.getOvertimeType())).count());
        stats.setByType(byType);

        // 获取项目加班统计(2026-06-13): 透传 accessibleUserIds,让 dept-pm / member 只能看到自己可访问集合内的项目分布
        List<OvertimeDTO.ProjectOvertimeStats> projectStats = getProjectStats(userId, startDate, endDate, accessibleUserIds);
        stats.setByProject(projectStats);

        return stats;
    }

    /**
     * 构造全零空 stats(2026-06-13): 用于可访问集为空/限定 userId 不在范围内时的快速返回
     */
    private OvertimeDTO.OvertimeStats buildEmptyStats() {
        OvertimeDTO.OvertimeStats empty = new OvertimeDTO.OvertimeStats();
        empty.setTotalRecords(0);
        empty.setTotalHours(BigDecimal.ZERO);
        empty.setTotalPeople(0);
        empty.setPendingApprovals(0);
        empty.setThisMonthHours(BigDecimal.ZERO);
        empty.setThisMonthPeople(0);
        empty.setByType(new OvertimeDTO.ByTypeStats());
        empty.setByProject(List.of());
        return empty;
    }

    /**
     * 获取用户加班统计
     *
     * 2026-06-13: 新增 userIds 透传。null = 不限(给 admin/PM);
     * 非空 = SQL 层 IN 守卫,只统计这些提交者的加班。
     */
    public List<OvertimeDTO.UserOvertimeStats> getUserStats(String projectId, LocalDate startDate, LocalDate endDate, List<String> userIds) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByUser(projectId, startDate, endDate, userIds);
        return results.stream().map(map -> {
            OvertimeDTO.UserOvertimeStats stats = new OvertimeDTO.UserOvertimeStats();
            stats.setUserId((String) map.get("userId"));
            stats.setUserName((String) map.get("userName"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取项目加班统计
     *
     * 2026-06-13: 新增 userIds 透传,语义同上。
     */
    public List<OvertimeDTO.ProjectOvertimeStats> getProjectStats(String userId, LocalDate startDate, LocalDate endDate, List<String> userIds) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByProject(userId, startDate, endDate, userIds);
        return results.stream().map(map -> {
            OvertimeDTO.ProjectOvertimeStats stats = new OvertimeDTO.ProjectOvertimeStats();
            stats.setProjectId((String) map.get("projectId"));
            stats.setProjectName((String) map.get("projectName"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            Object countObj = map.get("recordCount");
            if (countObj != null) {
                if (countObj instanceof Integer) {
                    stats.setRecordCount((Integer) countObj);
                } else if (countObj instanceof Long) {
                    stats.setRecordCount(((Long) countObj).intValue());
                }
            }
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取日期加班统计
     *
     * 2026-06-13: 新增 userIds 透传,语义同上。
     */
    public List<OvertimeDTO.DateOvertimeStats> getDateStats(String userId, String projectId, LocalDate startDate, LocalDate endDate, List<String> userIds) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByDate(userId, projectId, startDate, endDate, userIds);
        return results.stream().map(map -> {
            OvertimeDTO.DateOvertimeStats stats = new OvertimeDTO.DateOvertimeStats();
            stats.setDate((LocalDate) map.get("overtimeDate"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取类型加班统计
     *
     * 2026-06-13: 新增 userIds 透传,语义同上。
     */
    public List<OvertimeDTO.TypeOvertimeStats> getTypeStats(String userId, String projectId, LocalDate startDate, LocalDate endDate, List<String> userIds) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByType(userId, projectId, startDate, endDate, userIds);
        return results.stream().map(map -> {
            OvertimeDTO.TypeOvertimeStats stats = new OvertimeDTO.TypeOvertimeStats();
            stats.setOvertimeType((String) map.get("overtimeType"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取用户总加班时长
     */
    public BigDecimal getTotalHoursByUser(String userId) {
        BigDecimal hours = overtimeMapper.sumHoursByUserId(userId);
        return hours != null ? hours : BigDecimal.ZERO;
    }

    /**
     * 获取项目总加班时长
     */
    public BigDecimal getTotalHoursByProject(String projectId) {
        BigDecimal hours = overtimeMapper.sumHoursByProjectId(projectId);
        return hours != null ? hours : BigDecimal.ZERO;
    }

    /**
     * 获取待审批数量
     */
    public int getPendingCount(String projectId) {
        if (projectId != null) {
            return overtimeMapper.countPendingByProjectId(projectId);
        }
        return overtimeMapper.countByStatus("pending");
    }

    // ==================== 任务加班查询 ====================

    /**
     * 根据任务ID查询加班记录
     */
    public List<OvertimeRecord> getRecordsByTaskId(String taskId) {
        return overtimeMapper.selectByTaskId(taskId);
    }

    /**
     * 获取任务总加班时长
     */
    public BigDecimal getTotalHoursByTask(String taskId) {
        BigDecimal hours = overtimeMapper.sumHoursByTaskId(taskId);
        return hours != null ? hours : BigDecimal.ZERO;
    }

    /**
     * 获取项目中各任务的加班统计
     */
    public List<OvertimeDTO.TaskOvertimeStats> getTaskStats(String projectId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByTask(projectId, startDate, endDate);
        return results.stream().map(map -> {
            OvertimeDTO.TaskOvertimeStats stats = new OvertimeDTO.TaskOvertimeStats();
            stats.setTaskId((String) map.get("taskId"));
            stats.setTaskName((String) map.get("taskName"));
            stats.setAssigneeId((String) map.get("assigneeId"));
            stats.setAssigneeName((String) map.get("assigneeName"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            Object countObj = map.get("recordCount");
            if (countObj != null) {
                if (countObj instanceof Integer) {
                    stats.setRecordCount((Integer) countObj);
                } else if (countObj instanceof Long) {
                    stats.setRecordCount(((Long) countObj).intValue());
                }
            }
            return stats;
        }).collect(Collectors.toList());
    }

    // ==================== 辅助方法 ====================

    /**
     * 验证外键是否存在
     */
    private void validateForeignKeys(String userId, String projectId, String taskId) {
        // 验证用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在: " + userId);
        }

        // 验证项目
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在: " + projectId);
        }

        // 验证任务（可选）
        if (taskId != null && !taskId.trim().isEmpty()) {
            Task task = taskMapper.selectById(taskId);
            if (task == null) {
                throw new RuntimeException("任务不存在: " + taskId);
            }
        }
    }

    /**
     * 计算加班时长（小时）
     */
    private BigDecimal calculateHours(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            throw new RuntimeException("开始时间和结束时间不能为空");
        }

        long minutes = Duration.between(startTime, endTime).toMinutes();
        if (minutes <= 0) {
            throw new RuntimeException("结束时间必须晚于开始时间");
        }

        // 转换为小时，保留1位小数
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 1, RoundingMode.HALF_UP);

        return hours;
    }
}
