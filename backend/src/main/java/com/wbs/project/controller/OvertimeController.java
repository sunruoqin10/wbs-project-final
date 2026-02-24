package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.OvertimeDTO;
import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.service.OvertimeService;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 加班管理Controller
 */
@RestController
@RequestMapping("/overtime")
@RequiredArgsConstructor
public class OvertimeController {

    private final OvertimeService overtimeService;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    /**
     * 从请求中获取当前用户ID
     */
    private String getCurrentUserId(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        return userId;
    }

    /**
     * 根据权限过滤统计数据
     */
    private OvertimeDTO.OvertimeStats filterStatsByPermission(OvertimeDTO.OvertimeStats stats, String userId) {
        // 获取用户负责的项目ID列表
        List<Project> ownedProjects = projectMapper.selectByOwnerId(userId);
        List<String> ownedProjectIds = ownedProjects.stream()
                .map(Project::getId)
                .toList();

        // 如果没有负责的项目，返回空统计
        if (ownedProjectIds.isEmpty()) {
            OvertimeDTO.OvertimeStats emptyStats = new OvertimeDTO.OvertimeStats();
            emptyStats.setTotalRecords(0);
            emptyStats.setTotalHours(BigDecimal.ZERO);
            emptyStats.setTotalPeople(0);
            emptyStats.setPendingApprovals(0);
            emptyStats.setThisMonthHours(BigDecimal.ZERO);
            emptyStats.setThisMonthPeople(0);
            OvertimeDTO.ByTypeStats emptyByType = new OvertimeDTO.ByTypeStats();
            emptyStats.setByType(emptyByType);
            emptyStats.setByProject(List.of());
            return emptyStats;
        }

        // 过滤项目统计，只保留用户负责的项目
        List<OvertimeDTO.ProjectOvertimeStats> filteredByProject = stats.getByProject().stream()
                .filter(projectStat -> ownedProjectIds.contains(projectStat.getProjectId()))
                .toList();

        stats.setByProject(filteredByProject);

        return stats;
    }

    // ==================== CRUD API ====================

    /**
     * 获取加班记录列表
     * GET /api/overtime
     * 支持多条件查询：userId, projectId, status, startDate, endDate, overtimeType
     */
    @GetMapping
    public Result<List<OvertimeRecord>> getRecords(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String overtimeType,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        // 获取原始数据
        List<OvertimeRecord> records = overtimeService.getRecordsByCondition(
                userId, projectId, status, startDate, endDate, overtimeType);
        
        // 根据权限过滤数据
        if (currentUserId != null) {
            records = records.stream()
                    .filter(record -> overtimeService.hasPermission(
                        currentUserId, 
                        record.getProjectId(), 
                        record.getUserId()))
                    .toList();
        }
        
        return Result.success(records);
    }

    /**
     * 获取单条加班记录
     * GET /api/overtime/{id}
     */
    @GetMapping("/{id}")
    public Result<OvertimeRecord> getRecordById(@PathVariable String id, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        OvertimeRecord record = overtimeService.getRecordById(id);
        if (record == null) {
            return Result.error("加班记录不存在");
        }
        
        // 权限验证：用户只能访问其负责项目的加班记录或自己提交的记录
        if (currentUserId != null && !overtimeService.hasPermission(
                currentUserId, 
                record.getProjectId(), 
                record.getUserId())) {
            return Result.error("您没有权限查看此加班记录");
        }
        
        return Result.success(record);
    }

    /**
     * 新增加班记录
     * POST /api/overtime
     */
    @PostMapping
    public Result<OvertimeRecord> createRecord(@RequestBody OvertimeDTO.CreateRequest request) {
        OvertimeRecord record = overtimeService.createRecord(request);
        return Result.success("加班记录创建成功", record);
    }

    /**
     * 更新加班记录
     * PUT /api/overtime/{id}
     */
    @PutMapping("/{id}")
    public Result<OvertimeRecord> updateRecord(@PathVariable String id, @RequestBody OvertimeDTO.UpdateRequest request, HttpServletRequest httpRequest) {
        String currentUserId = getCurrentUserId(httpRequest);
        
        // 获取现有记录进行权限验证
        OvertimeRecord existingRecord = overtimeService.getRecordById(id);
        if (existingRecord == null) {
            return Result.error("加班记录不存在");
        }
        
        // 权限验证：用户只能更新其负责项目的加班记录或自己提交的记录
        if (currentUserId != null && !overtimeService.hasPermission(
                currentUserId, 
                existingRecord.getProjectId(), 
                existingRecord.getUserId())) {
            return Result.error("您没有权限更新此加班记录");
        }
        
        OvertimeRecord record = overtimeService.updateRecord(id, request);
        return Result.success("加班记录更新成功", record);
    }

    /**
     * 删除加班记录
     * DELETE /api/overtime/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRecord(@PathVariable String id, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        // 获取现有记录进行权限验证
        OvertimeRecord existingRecord = overtimeService.getRecordById(id);
        if (existingRecord == null) {
            return Result.error("加班记录不存在");
        }
        
        // 权限验证：用户只能删除其负责项目的加班记录或自己提交的记录
        if (currentUserId != null && !overtimeService.hasPermission(
                currentUserId, 
                existingRecord.getProjectId(), 
                existingRecord.getUserId())) {
            return Result.error("您没有权限删除此加班记录");
        }
        
        overtimeService.deleteRecord(id);
        return Result.success();
    }

    // ==================== 审批 API ====================

    /**
     * 审批加班申请
     * PUT /api/overtime/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public Result<OvertimeRecord> approveRecord(@PathVariable String id, @RequestBody OvertimeDTO.ApproveRequest request) {
        OvertimeRecord record = overtimeService.approveRecord(id, request);
        String message = request.getApproved() ? "加班申请已批准" : "加班申请已拒绝";
        return Result.success(message, record);
    }

    // ==================== 统计 API ====================

    /**
     * 获取加班统计数据
     * GET /api/overtime/stats
     */
    @GetMapping("/stats")
    public Result<OvertimeDTO.OvertimeStats> getStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        
        // 如果指定了userId，验证是否有权限查看该用户的统计
        if (userId != null && !userId.equals(currentUserId)) {
            // 检查是否有管理权限
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser == null || (!"admin".equals(currentUser.getRole()) && !"project-manager".equals(currentUser.getRole()))) {
                return Result.error("您没有权限查看该用户的统计");
            }
        }
        
        OvertimeDTO.OvertimeStats stats = overtimeService.getStats(userId, projectId, startDate, endDate);
        
        // 如果不是管理员和项目经理，过滤统计结果只显示用户有权限的项目
        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && !"admin".equals(currentUser.getRole()) && !"project-manager".equals(currentUser.getRole())) {
                stats = filterStatsByPermission(stats, currentUserId);
            }
        }
        
        return Result.success(stats);
    }

    /**
     * 获取用户加班统计
     * GET /api/overtime/stats/users
     */
    @GetMapping("/stats/users")
    public Result<List<OvertimeDTO.UserOvertimeStats>> getUserStats(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OvertimeDTO.UserOvertimeStats> stats = overtimeService.getUserStats(projectId, startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 获取项目加班统计
     * GET /api/overtime/stats/projects
     */
    @GetMapping("/stats/projects")
    public Result<List<OvertimeDTO.ProjectOvertimeStats>> getProjectStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OvertimeDTO.ProjectOvertimeStats> stats = overtimeService.getProjectStats(userId, startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 获取日期加班统计
     * GET /api/overtime/stats/dates
     */
    @GetMapping("/stats/dates")
    public Result<List<OvertimeDTO.DateOvertimeStats>> getDateStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OvertimeDTO.DateOvertimeStats> stats = overtimeService.getDateStats(userId, projectId, startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 获取类型加班统计
     * GET /api/overtime/stats/types
     */
    @GetMapping("/stats/types")
    public Result<List<OvertimeDTO.TypeOvertimeStats>> getTypeStats(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OvertimeDTO.TypeOvertimeStats> stats = overtimeService.getTypeStats(userId, projectId, startDate, endDate);
        return Result.success(stats);
    }

    /**
     * 获取用户总加班时长
     * GET /api/overtime/total-hours/user/{userId}
     */
    @GetMapping("/total-hours/user/{userId}")
    public Result<BigDecimal> getTotalHoursByUser(@PathVariable String userId) {
        BigDecimal hours = overtimeService.getTotalHoursByUser(userId);
        return Result.success(hours);
    }

    /**
     * 获取项目总加班时长
     * GET /api/overtime/total-hours/project/{projectId}
     */
    @GetMapping("/total-hours/project/{projectId}")
    public Result<BigDecimal> getTotalHoursByProject(@PathVariable String projectId) {
        BigDecimal hours = overtimeService.getTotalHoursByProject(projectId);
        return Result.success(hours);
    }

    /**
     * 获取待审批数量
     * GET /api/overtime/pending-count
     */
    @GetMapping("/pending-count")
    public Result<Integer> getPendingCount(@RequestParam(required = false) String projectId) {
        int count = overtimeService.getPendingCount(projectId);
        return Result.success(count);
    }

    // ==================== 快捷查询 API ====================

    /**
     * 获取当前用户的加班记录
     * GET /api/overtime/my/{userId}
     */
    @GetMapping("/my/{userId}")
    public Result<List<OvertimeRecord>> getMyRecords(@PathVariable String userId) {
        List<OvertimeRecord> records = overtimeService.getRecordsByUserId(userId);
        return Result.success(records);
    }

    /**
     * 获取项目的加班记录
     * GET /api/overtime/project/{projectId}
     */
    @GetMapping("/project/{projectId}")
    public Result<List<OvertimeRecord>> getProjectRecords(@PathVariable String projectId) {
        List<OvertimeRecord> records = overtimeService.getRecordsByProjectId(projectId);
        return Result.success(records);
    }

    /**
     * 获取待审批的加班记录
     * GET /api/overtime/pending
     */
    @GetMapping("/pending")
    public Result<List<OvertimeRecord>> getPendingRecords(
            @RequestParam(required = false) String projectId) {
        List<OvertimeRecord> records = overtimeService.getRecordsByCondition(
                null, projectId, "pending", null, null, null);
        return Result.success(records);
    }

    // ==================== 任务加班查询 API ====================

    /**
     * 获取任务的加班记录
     * GET /api/overtime/task/{taskId}
     */
    @GetMapping("/task/{taskId}")
    public Result<List<OvertimeRecord>> getTaskRecords(@PathVariable String taskId) {
        List<OvertimeRecord> records = overtimeService.getRecordsByTaskId(taskId);
        return Result.success(records);
    }

    /**
     * 获取任务总加班时长
     * GET /api/overtime/total-hours/task/{taskId}
     */
    @GetMapping("/total-hours/task/{taskId}")
    public Result<BigDecimal> getTotalHoursByTask(@PathVariable String taskId) {
        BigDecimal hours = overtimeService.getTotalHoursByTask(taskId);
        return Result.success(hours);
    }

    /**
     * 获取项目中各任务的加班统计
     * GET /api/overtime/stats/tasks
     */
    @GetMapping("/stats/tasks")
    public Result<List<OvertimeDTO.TaskOvertimeStats>> getTaskStats(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        List<OvertimeDTO.TaskOvertimeStats> stats = overtimeService.getTaskStats(projectId, startDate, endDate);
        return Result.success(stats);
    }
}
