package com.wbs.project.service;

import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.entity.WeeklyReportComment;
import com.wbs.project.enums.ReportStatus;
import com.wbs.project.mapper.WeeklyReportMapper;
import com.wbs.project.mapper.WeeklyReportCommentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WeeklyReportService {

    private final WeeklyReportMapper weeklyReportMapper;
    private final WeeklyReportCommentMapper commentMapper;

    public List<WeeklyReport> getAllReports() {
        return weeklyReportMapper.selectAll();
    }

    public WeeklyReport getReportById(String id) {
        return weeklyReportMapper.selectById(id);
    }

    public List<WeeklyReport> getReportsByUserId(String userId) {
        return weeklyReportMapper.selectByUserId(userId);
    }

    public List<WeeklyReport> getReportsByProjectId(String projectId) {
        return weeklyReportMapper.selectByProjectId(projectId);
    }

    public WeeklyReport getReportByUserAndWeek(String userId, LocalDate weekStart) {
        return weeklyReportMapper.selectByUserAndWeek(userId, weekStart);
    }

    public WeeklyReport getCurrentWeekReport(String userId) {
        LocalDate today = LocalDate.now();
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate sunday = monday.plusDays(6);
        return getReportByUserAndWeek(userId, monday);
    }

    @Transactional
    public WeeklyReport createReport(WeeklyReport report) {
        if (report.getId() == null || report.getId().isEmpty()) {
            report.setId(UUID.randomUUID().toString());
        }
        if (report.getStatus() == null) {
            report.setStatus(ReportStatus.DRAFT.getCode());
        }
        report.setCreatedAt(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        weeklyReportMapper.insert(report);
        return report;
    }

    @Transactional
    public WeeklyReport updateReport(WeeklyReport report) {
        WeeklyReport existing = getReportById(report.getId());
        if (existing == null) {
            throw new RuntimeException("周报不存在");
        }
        if (!ReportStatus.DRAFT.getCode().equals(existing.getStatus()) &&
            !ReportStatus.REJECTED.getCode().equals(existing.getStatus())) {
            throw new RuntimeException("只能编辑草稿或已拒绝状态的周报");
        }
        report.setUpdatedAt(LocalDateTime.now());
        weeklyReportMapper.update(report);
        return report;
    }

    @Transactional
    public void deleteReport(String id) {
        WeeklyReport report = getReportById(id);
        if (report == null) {
            throw new RuntimeException("周报不存在");
        }
        commentMapper.deleteByReportId(id);
        weeklyReportMapper.deleteById(id);
    }

    @Transactional
    public WeeklyReport submitReport(String id) {
        WeeklyReport report = getReportById(id);
        if (report == null) {
            throw new RuntimeException("周报不存在");
        }
        if (!ReportStatus.DRAFT.getCode().equals(report.getStatus())) {
            throw new RuntimeException("只能提交草稿状态的周报");
        }
        report.setStatus(ReportStatus.SUBMITTED.getCode());
        report.setSubmitTime(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        weeklyReportMapper.update(report);
        return report;
    }

    @Transactional
    public WeeklyReport approveReport(String id, String approverId, String approveComment, boolean approved) {
        WeeklyReport report = getReportById(id);
        if (report == null) {
            throw new RuntimeException("周报不存在");
        }
        if (!ReportStatus.SUBMITTED.getCode().equals(report.getStatus())) {
            throw new RuntimeException("只能审批已提交状态的周报");
        }
        report.setStatus(approved ? ReportStatus.APPROVED.getCode() : ReportStatus.REJECTED.getCode());
        report.setApproverId(approverId);
        report.setApproveComment(approveComment);
        report.setApproveTime(LocalDateTime.now());
        report.setUpdatedAt(LocalDateTime.now());
        weeklyReportMapper.update(report);
        return report;
    }

    public int countByUserId(String userId) {
        return weeklyReportMapper.countByUserId(userId);
    }

    public int countByProjectId(String projectId) {
        return weeklyReportMapper.countByProjectId(projectId);
    }

    public int countByStatus(String status) {
        return weeklyReportMapper.countByStatus(status);
    }
}
