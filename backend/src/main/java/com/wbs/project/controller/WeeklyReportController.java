package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.entity.WeeklyReportComment;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.service.WeeklyReportCommentService;
import com.wbs.project.service.WeeklyReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/weekly-reports")
@RequiredArgsConstructor
public class WeeklyReportController {

    private final WeeklyReportService weeklyReportService;
    private final WeeklyReportCommentService commentService;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;

    private String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    private boolean hasPermission(String currentUserId, WeeklyReport report) {
        if (currentUserId == null) return true;
        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) return false;
        if ("admin".equals(currentUser.getRole())) return true;
        if ("project-manager".equals(currentUser.getRole())) {
            if (report.getProjectId() != null) {
                Project project = projectMapper.selectById(report.getProjectId());
                return project != null && project.getOwnerId().equals(currentUserId);
            }
            return false;
        }
        return currentUserId.equals(report.getUserId());
    }

    @GetMapping
    public Result<List<WeeklyReport>> getReports(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        List<WeeklyReport> reports = weeklyReportService.getAllReports();

        if (currentUserId != null) {
            reports = reports.stream()
                    .filter(report -> hasPermission(currentUserId, report))
                    .toList();
        }

        if (userId != null) {
            reports = reports.stream()
                    .filter(report -> userId.equals(report.getUserId()))
                    .toList();
        }

        if (projectId != null) {
            reports = reports.stream()
                    .filter(report -> projectId.equals(report.getProjectId()))
                    .toList();
        }

        if (status != null) {
            reports = reports.stream()
                    .filter(report -> status.equals(report.getStatus()))
                    .toList();
        }

        if (startDate != null) {
            reports = reports.stream()
                    .filter(report -> !report.getWeekStart().isBefore(startDate))
                    .toList();
        }

        if (endDate != null) {
            reports = reports.stream()
                    .filter(report -> !report.getWeekEnd().isAfter(endDate))
                    .toList();
        }

        return Result.success(reports);
    }

    @GetMapping("/{id}")
    public Result<WeeklyReport> getReportById(@PathVariable String id, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport report = weeklyReportService.getReportById(id);

        if (report == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId != null && !hasPermission(currentUserId, report)) {
            return Result.error("您没有权限查看此周报");
        }

        return Result.success(report);
    }

    @PostMapping
    public Result<WeeklyReport> createReport(@RequestBody WeeklyReport report) {
        if (report.getUserId() == null || report.getUserId().isEmpty()) {
            return Result.error("提交人ID不能为空");
        }
        if (report.getCompletedWork() == null || report.getCompletedWork().isEmpty()) {
            return Result.error("本周完成工作不能为空");
        }
        if (report.getNextWeekPlan() == null || report.getNextWeekPlan().isEmpty()) {
            return Result.error("下周计划不能为空");
        }

        WeeklyReport created = weeklyReportService.createReport(report);
        return Result.success("周报创建成功", created);
    }

    @PutMapping("/{id}")
    public Result<WeeklyReport> updateReport(@PathVariable String id, @RequestBody WeeklyReport report, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport existing = weeklyReportService.getReportById(id);

        if (existing == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId != null && !hasPermission(currentUserId, existing)) {
            return Result.error("您没有权限编辑此周报");
        }

        report.setId(id);
        WeeklyReport updated = weeklyReportService.updateReport(report);
        return Result.success("周报更新成功", updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteReport(@PathVariable String id, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport existing = weeklyReportService.getReportById(id);

        if (existing == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId != null && !hasPermission(currentUserId, existing)) {
            return Result.error("您没有权限删除此周报");
        }

        weeklyReportService.deleteReport(id);
        return Result.success();
    }

    @PostMapping("/{id}/submit")
    public Result<WeeklyReport> submitReport(@PathVariable String id, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport existing = weeklyReportService.getReportById(id);

        if (existing == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId != null && !currentUserId.equals(existing.getUserId())) {
            return Result.error("您没有权限提交此周报");
        }

        WeeklyReport submitted = weeklyReportService.submitReport(id);
        return Result.success("周报提交成功", submitted);
    }

    @PostMapping("/{id}/approve")
    public Result<WeeklyReport> approveReport(
            @PathVariable String id,
            @RequestBody ApproveRequest approveRequest,
            HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport existing = weeklyReportService.getReportById(id);

        if (existing == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null || (!"admin".equals(currentUser.getRole()) && !"project-manager".equals(currentUser.getRole()))) {
            return Result.error("您没有权限审批周报");
        }

        if (existing.getProjectId() != null) {
            Project project = projectMapper.selectById(existing.getProjectId());
            if (project == null || !project.getOwnerId().equals(currentUserId)) {
                return Result.error("您只能审批您负责项目的周报");
            }
        }

        WeeklyReport approved = weeklyReportService.approveReport(
                id,
                currentUserId,
                approveRequest.getApproveComment(),
                approveRequest.getApproved()
        );

        String message = approveRequest.getApproved() ? "周报已审批通过" : "周报已拒绝";
        return Result.success(message, approved);
    }

    @GetMapping("/my")
    public Result<List<WeeklyReport>> getMyReports(@RequestParam String userId, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        if (currentUserId != null && !currentUserId.equals(userId)) {
            return Result.error("您没有权限查看该用户的周报");
        }
        List<WeeklyReport> reports = weeklyReportService.getReportsByUserId(userId);
        return Result.success(reports);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<WeeklyReport>> getProjectReports(@PathVariable String projectId, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        List<WeeklyReport> reports = weeklyReportService.getReportsByProjectId(projectId);

        if (currentUserId != null) {
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null && !"admin".equals(currentUser.getRole())) {
                Project project = projectMapper.selectById(projectId);
                if (project == null || (!project.getOwnerId().equals(currentUserId) &&
                        !project.getMemberIds().contains(currentUserId))) {
                    return Result.error("您没有权限查看该项目的周报");
                }
                reports = reports.stream()
                        .filter(report -> currentUserId.equals(report.getUserId()))
                        .toList();
            }
        }

        return Result.success(reports);
    }

    @GetMapping("/current-week")
    public Result<WeeklyReport> getCurrentWeekReport(@RequestParam String userId, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        if (currentUserId != null && !currentUserId.equals(userId)) {
            return Result.error("您没有权限查看该用户的周报");
        }
        WeeklyReport report = weeklyReportService.getCurrentWeekReport(userId);
        return Result.success(report);
    }

    @GetMapping("/{reportId}/comments")
    public Result<List<WeeklyReportComment>> getComments(@PathVariable String reportId, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport report = weeklyReportService.getReportById(reportId);

        if (report == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId != null && !hasPermission(currentUserId, report)) {
            return Result.error("您没有权限查看此周报的评论");
        }

        List<WeeklyReportComment> comments = commentService.getCommentsByReportId(reportId);
        return Result.success(comments);
    }

    @PostMapping("/{reportId}/comments")
    public Result<WeeklyReportComment> addComment(@PathVariable String reportId, @RequestBody WeeklyReportComment comment, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReport report = weeklyReportService.getReportById(reportId);

        if (report == null) {
            return Result.error("周报不存在");
        }

        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        if (currentUserId != null && !hasPermission(currentUserId, report)) {
            return Result.error("您没有权限对此周报添加评论");
        }

        if (comment.getContent() == null || comment.getContent().isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        comment.setReportId(reportId);
        comment.setUserId(currentUserId);
        WeeklyReportComment created = commentService.createComment(comment);
        return Result.success("评论添加成功", created);
    }

    @DeleteMapping("/comments/{commentId}")
    public Result<Void> deleteComment(@PathVariable String commentId, HttpServletRequest request) {
        String currentUserId = getCurrentUserId(request);
        WeeklyReportComment comment = commentService.getCommentsByReportId(commentId).stream()
                .filter(c -> c.getId().equals(commentId))
                .findFirst()
                .orElse(null);

        if (comment == null) {
            comment = new WeeklyReportComment();
            comment.setId(commentId);
        }

        if (currentUserId == null) {
            return Result.error("请先登录");
        }

        User currentUser = userMapper.selectById(currentUserId);
        if (currentUser == null) {
            return Result.error("用户不存在");
        }

        if (!currentUserId.equals(comment.getUserId()) && !"admin".equals(currentUser.getRole())) {
            return Result.error("您没有权限删除此评论");
        }

        commentService.deleteComment(commentId);
        return Result.success();
    }

    public static class ApproveRequest {
        private Boolean approved;
        private String approveComment;

        public Boolean getApproved() {
            return approved;
        }

        public void setApproved(Boolean approved) {
            this.approved = approved;
        }

        public String getApproveComment() {
            return approveComment;
        }

        public void setApproveComment(String approveComment) {
            this.approveComment = approveComment;
        }
    }
}
