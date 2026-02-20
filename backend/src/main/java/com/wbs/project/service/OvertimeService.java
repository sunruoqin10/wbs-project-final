package com.wbs.project.service;

import com.wbs.project.dto.OvertimeDTO;
import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.OvertimeMapper;
import com.wbs.project.mapper.ProjectMapper;
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
    private final TaskMapper taskMapper;

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
        return overtimeMapper.selectByCondition(userId, projectId, status, startDate, endDate, overtimeType);
    }

    /**
     * 创建加班记录
     */
    @Transactional
    public OvertimeRecord createRecord(OvertimeDTO.CreateRequest request) {
        // 验证外键
        validateForeignKeys(request.getUserId(), request.getProjectId(), request.getTaskId());

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
        return overtimeMapper.selectById(record.getId());
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

        // 只有待审批状态的记录可以修改
        if (!"pending".equals(existing.getStatus())) {
            throw new RuntimeException("只有待审批状态的加班记录可以修改");
        }

        // 验证外键
        validateForeignKeys(existing.getUserId(), request.getProjectId(), request.getTaskId());

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

        // 只有待审批状态的记录可以删除
        if (!"pending".equals(record.getStatus())) {
            throw new RuntimeException("只有待审批状态的加班记录可以删除");
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

        if (request.getApproved()) {
            // 批准
            record.setStatus("approved");
            record.setApproverId(request.getApproverId());
            record.setApprovedAt(LocalDateTime.now());
        } else {
            // 拒绝
            if (request.getRejectReason() == null || request.getRejectReason().trim().isEmpty()) {
                throw new RuntimeException("拒绝时必须填写拒绝原因");
            }
            record.setStatus("rejected");
            record.setApproverId(request.getApproverId());
            record.setApprovedAt(LocalDateTime.now());
            record.setRejectReason(request.getRejectReason());
        }

        record.setUpdatedAt(LocalDateTime.now());
        overtimeMapper.update(record);
        return overtimeMapper.selectById(id);
    }

    /**
     * 验证审批人权限
     */
    private void validateApprover(String approverId, String projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new RuntimeException("项目不存在");
        }

        // 项目负责人可以审批
        if (approverId.equals(project.getOwnerId())) {
            return;
        }

        // TODO: 可以扩展其他审批权限逻辑，如部门经理、HR等

        throw new RuntimeException("您没有权限审批此加班申请");
    }

    // ==================== 统计计算 ====================

    /**
     * 获取加班统计信息
     */
    public OvertimeDTO.OvertimeStats getStats(String userId, String projectId, LocalDate startDate, LocalDate endDate) {
        List<OvertimeRecord> records = overtimeMapper.selectByCondition(userId, projectId, null, startDate, endDate, null);

        OvertimeDTO.OvertimeStats stats = new OvertimeDTO.OvertimeStats();
        stats.setTotalRecords(records.size());
        stats.setPendingRecords((int) records.stream().filter(r -> "pending".equals(r.getStatus())).count());
        stats.setApprovedRecords((int) records.stream().filter(r -> "approved".equals(r.getStatus())).count());
        stats.setRejectedRecords((int) records.stream().filter(r -> "rejected".equals(r.getStatus())).count());

        stats.setTotalHours(records.stream()
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

        return stats;
    }

    /**
     * 获取用户加班统计
     */
    public List<OvertimeDTO.UserOvertimeStats> getUserStats(String projectId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByUser(projectId, startDate, endDate);
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
     */
    public List<OvertimeDTO.ProjectOvertimeStats> getProjectStats(String userId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByProject(userId, startDate, endDate);
        return results.stream().map(map -> {
            OvertimeDTO.ProjectOvertimeStats stats = new OvertimeDTO.ProjectOvertimeStats();
            stats.setProjectId((String) map.get("projectId"));
            stats.setProjectName((String) map.get("projectName"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取日期加班统计
     */
    public List<OvertimeDTO.DateOvertimeStats> getDateStats(String userId, String projectId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByDate(userId, projectId, startDate, endDate);
        return results.stream().map(map -> {
            OvertimeDTO.DateOvertimeStats stats = new OvertimeDTO.DateOvertimeStats();
            stats.setDate((LocalDate) map.get("overtimeDate"));
            stats.setTotalHours((BigDecimal) map.get("totalHours"));
            return stats;
        }).collect(Collectors.toList());
    }

    /**
     * 获取类型加班统计
     */
    public List<OvertimeDTO.TypeOvertimeStats> getTypeStats(String userId, String projectId, LocalDate startDate, LocalDate endDate) {
        List<Map<String, Object>> results = overtimeMapper.sumHoursGroupByType(userId, projectId, startDate, endDate);
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
