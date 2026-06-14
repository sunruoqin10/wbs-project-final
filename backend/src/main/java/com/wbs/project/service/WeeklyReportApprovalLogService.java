package com.wbs.project.service;

import com.wbs.project.dto.WeeklyReportApprovalLogDTO;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.User;
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.entity.WeeklyReportApprovalLog;
import com.wbs.project.mapper.ProjectMapper;
import com.wbs.project.mapper.UserMapper;
import com.wbs.project.mapper.WeeklyReportApprovalLogMapper;
import com.wbs.project.mapper.WeeklyReportMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class WeeklyReportApprovalLogService {

    private final WeeklyReportApprovalLogMapper logMapper;
    private final UserMapper userMapper;
    private final WeeklyReportMapper weeklyReportMapper;
    private final ProjectMapper projectMapper;

    /** 主流程已成功;写日志失败仅 ERROR,不抛出 */
    public void log(String reportId, String approverId, String action, String comment) {
        try {
            WeeklyReport r = weeklyReportMapper.selectById(reportId);
            WeeklyReportApprovalLog row = new WeeklyReportApprovalLog();
            row.setReportId(reportId);
            row.setApproverId(approverId);
            row.setApproverRole(resolveApproverRole(approverId,
                    r == null ? null : r.getProjectId()));
            row.setAction(action);
            row.setComment(comment);
            row.setCreatedAt(LocalDateTime.now());
            logMapper.insert(row);
        } catch (Exception e) {
            log.error("写入周报审批日志失败 reportId={} approver={}", reportId, approverId, e);
        }
    }

    public List<WeeklyReportApprovalLogDTO> listByReport(String reportId) {
        return logMapper.selectByReportId(reportId);
    }

    /** 与 OvertimeService.resolveApproverRole:376-388 同形(顺序不可调) */
    private String resolveApproverRole(String approverId, String projectId) {
        User approver = userMapper.selectById(approverId);
        if (approver == null) return "unknown";
        String role = approver.getRole();
        if ("admin".equals(role) || "project-manager".equals(role)) {
            return role;
        }
        if (projectId != null) {
            Project project = projectMapper.selectById(projectId);
            if (project != null && approverId.equals(project.getOwnerId())) {
                return "project-owner";
            }
        }
        return "dept-project-manager";
    }
}
