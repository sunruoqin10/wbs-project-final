package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.OvertimeDTO;
import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.service.OvertimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(required = false) String overtimeType) {
        List<OvertimeRecord> records = overtimeService.getRecordsByCondition(
                userId, projectId, status, startDate, endDate, overtimeType);
        return Result.success(records);
    }

    /**
     * 获取单条加班记录
     * GET /api/overtime/{id}
     */
    @GetMapping("/{id}")
    public Result<OvertimeRecord> getRecordById(@PathVariable String id) {
        OvertimeRecord record = overtimeService.getRecordById(id);
        if (record == null) {
            return Result.error("加班记录不存在");
        }
        return Result.success(record);
    }

    /**
     * 新增加班记录
     * POST /api/overtime
     */
    @PostMapping
    public Result<OvertimeRecord> createRecord(@RequestBody OvertimeDTO.CreateRequest request) {
        try {
            OvertimeRecord record = overtimeService.createRecord(request);
            return Result.success("加班记录创建成功", record);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新加班记录
     * PUT /api/overtime/{id}
     */
    @PutMapping("/{id}")
    public Result<OvertimeRecord> updateRecord(@PathVariable String id, @RequestBody OvertimeDTO.UpdateRequest request) {
        try {
            OvertimeRecord record = overtimeService.updateRecord(id, request);
            return Result.success("加班记录更新成功", record);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除加班记录
     * DELETE /api/overtime/{id}
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRecord(@PathVariable String id) {
        try {
            overtimeService.deleteRecord(id);
            return Result.success();
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    // ==================== 审批 API ====================

    /**
     * 审批加班申请
     * PUT /api/overtime/{id}/approve
     */
    @PutMapping("/{id}/approve")
    public Result<OvertimeRecord> approveRecord(@PathVariable String id, @RequestBody OvertimeDTO.ApproveRequest request) {
        try {
            OvertimeRecord record = overtimeService.approveRecord(id, request);
            String message = request.getApproved() ? "加班申请已批准" : "加班申请已拒绝";
            return Result.success(message, record);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
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
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        OvertimeDTO.OvertimeStats stats = overtimeService.getStats(userId, projectId, startDate, endDate);
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
