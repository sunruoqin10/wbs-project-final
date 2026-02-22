package com.wbs.project.service;

import com.wbs.project.entity.OvertimeRecord;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final EmailService emailService;
    private final UserService userService;

    public void notifyProjectCreated(Project project, List<String> memberIds) {
        log.info("Notifying project created: {}", project.getName());
        for (String memberId : memberIds) {
            User user = userService.getUserById(memberId);
            if (user != null && user.getEmail() != null) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("projectName", project.getName());
                variables.put("projectDescription", project.getDescription());
                variables.put("userName", user.getName());
                emailService.sendEmail(
                    user.getEmail(),
                    "【WBS系统】您已加入新项目：" + project.getName(),
                    "email/project-created",
                    variables
                );
            }
        }
    }

    public void notifyMemberAdded(Project project, String newMemberId) {
        log.info("Notifying member added to project: {}", project.getName());
        User user = userService.getUserById(newMemberId);
        if (user != null && user.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectName", project.getName());
            variables.put("userName", user.getName());
            emailService.sendEmail(
                user.getEmail(),
                "【WBS系统】您已加入项目：" + project.getName(),
                "email/member-added",
                variables
            );
        }
    }

    public void notifyMemberRemoved(Project project, String removedMemberId) {
        log.info("Notifying member removed from project: {}", project.getName());
        User user = userService.getUserById(removedMemberId);
        if (user != null && user.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("projectName", project.getName());
            variables.put("userName", user.getName());
            emailService.sendEmail(
                user.getEmail(),
                "【WBS系统】您已从项目移除：" + project.getName(),
                "email/member-removed",
                variables
            );
        }
    }

    public void notifyProjectStatusChanged(Project project, String oldStatus, String newStatus, List<String> memberIds) {
        log.info("Notifying project status changed: {} -> {}", oldStatus, newStatus);
        for (String memberId : memberIds) {
            User user = userService.getUserById(memberId);
            if (user != null && user.getEmail() != null) {
                Map<String, Object> variables = new HashMap<>();
                variables.put("projectName", project.getName());
                variables.put("oldStatus", getStatusLabel(oldStatus));
                variables.put("newStatus", getStatusLabel(newStatus));
                variables.put("userName", user.getName());
                emailService.sendEmail(
                    user.getEmail(),
                    "【WBS系统】项目状态变更：" + project.getName(),
                    "email/project-status-changed",
                    variables
                );
            }
        }
    }

    public void notifyTaskAssigned(Task task, User assignee) {
        log.info("Notifying task assigned: {}", task.getTitle());
        if (assignee != null && assignee.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("taskTitle", task.getTitle());
            variables.put("taskDescription", task.getDescription());
            variables.put("userName", assignee.getName());
            variables.put("dueDate", task.getEndDate() != null ? task.getEndDate().toString() : "未设置");
            emailService.sendEmail(
                assignee.getEmail(),
                "【WBS系统】您有新任务：" + task.getTitle(),
                "email/task-assigned",
                variables
            );
        }
    }

    public void notifyTaskReassigned(Task task, User oldAssignee, User newAssignee) {
        log.info("Notifying task reassigned: {}", task.getTitle());
        if (oldAssignee != null && oldAssignee.getEmail() != null) {
            Map<String, Object> oldVariables = new HashMap<>();
            oldVariables.put("taskTitle", task.getTitle());
            oldVariables.put("userName", oldAssignee.getName());
            oldVariables.put("newAssigneeName", newAssignee != null ? newAssignee.getName() : "未知");
            emailService.sendEmail(
                oldAssignee.getEmail(),
                "【WBS系统】任务已重新分配：" + task.getTitle(),
                "email/task-unassigned",
                oldVariables
            );
        }
        if (newAssignee != null && newAssignee.getEmail() != null) {
            Map<String, Object> newVariables = new HashMap<>();
            newVariables.put("taskTitle", task.getTitle());
            newVariables.put("taskDescription", task.getDescription());
            newVariables.put("userName", newAssignee.getName());
            newVariables.put("dueDate", task.getEndDate() != null ? task.getEndDate().toString() : "未设置");
            emailService.sendEmail(
                newAssignee.getEmail(),
                "【WBS系统】您有新任务：" + task.getTitle(),
                "email/task-assigned",
                newVariables
            );
        }
    }

    public void notifyTaskStatusChanged(Task task, String oldStatus, String newStatus, User assignee, User projectOwner) {
        log.info("Notifying task status changed: {} -> {}", oldStatus, newStatus);
        if (assignee != null && assignee.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("taskTitle", task.getTitle());
            variables.put("oldStatus", getTaskStatusLabel(oldStatus));
            variables.put("newStatus", getTaskStatusLabel(newStatus));
            variables.put("userName", assignee.getName());
            emailService.sendEmail(
                assignee.getEmail(),
                "【WBS系统】任务状态变更：" + task.getTitle(),
                "email/task-status-changed",
                variables
            );
        }
        if (projectOwner != null && projectOwner.getEmail() != null && 
            (assignee == null || !projectOwner.getId().equals(assignee.getId()))) {
            Map<String, Object> ownerVariables = new HashMap<>();
            ownerVariables.put("taskTitle", task.getTitle());
            ownerVariables.put("oldStatus", getTaskStatusLabel(oldStatus));
            ownerVariables.put("newStatus", getTaskStatusLabel(newStatus));
            ownerVariables.put("userName", projectOwner.getName());
            ownerVariables.put("assigneeName", assignee != null ? assignee.getName() : "未分配");
            emailService.sendEmail(
                projectOwner.getEmail(),
                "【WBS系统】任务状态变更：" + task.getTitle(),
                "email/task-status-changed-owner",
                ownerVariables
            );
        }
    }

    public void notifyTaskDelayed(Task task, User assignee, User projectOwner) {
        log.info("Notifying task delayed: {}", task.getTitle());
        if (assignee != null && assignee.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("taskTitle", task.getTitle());
            variables.put("delayedDays", task.getDelayedDays() != null ? task.getDelayedDays() : 0);
            variables.put("userName", assignee.getName());
            emailService.sendEmail(
                assignee.getEmail(),
                "【WBS系统】任务延期预警：" + task.getTitle(),
                "email/task-delayed",
                variables
            );
        }
        if (projectOwner != null && projectOwner.getEmail() != null && 
            (assignee == null || !projectOwner.getId().equals(assignee.getId()))) {
            Map<String, Object> ownerVariables = new HashMap<>();
            ownerVariables.put("taskTitle", task.getTitle());
            ownerVariables.put("delayedDays", task.getDelayedDays() != null ? task.getDelayedDays() : 0);
            ownerVariables.put("userName", projectOwner.getName());
            ownerVariables.put("assigneeName", assignee != null ? assignee.getName() : "未分配");
            emailService.sendEmail(
                projectOwner.getEmail(),
                "【WBS系统】任务延期预警：" + task.getTitle(),
                "email/task-delayed-owner",
                ownerVariables
            );
        }
    }

    public void notifyTaskCompleted(Task task, User assignee, User projectOwner) {
        log.info("Notifying task completed: {}", task.getTitle());
        if (assignee != null && assignee.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("taskTitle", task.getTitle());
            variables.put("userName", assignee.getName());
            emailService.sendEmail(
                assignee.getEmail(),
                "【WBS系统】任务已完成：" + task.getTitle(),
                "email/task-completed",
                variables
            );
        }
        if (projectOwner != null && projectOwner.getEmail() != null && 
            (assignee == null || !projectOwner.getId().equals(assignee.getId()))) {
            Map<String, Object> ownerVariables = new HashMap<>();
            ownerVariables.put("taskTitle", task.getTitle());
            ownerVariables.put("userName", projectOwner.getName());
            ownerVariables.put("assigneeName", assignee != null ? assignee.getName() : "未分配");
            emailService.sendEmail(
                projectOwner.getEmail(),
                "【WBS系统】任务已完成：" + task.getTitle(),
                "email/task-completed-owner",
                ownerVariables
            );
        }
    }

    public void notifyOvertimeSubmitted(OvertimeRecord record, User applicant, User projectOwner) {
        log.info("Notifying overtime submitted by: {}", applicant != null ? applicant.getName() : "unknown");
        if (projectOwner != null && projectOwner.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("applicantName", applicant != null ? applicant.getName() : "未知");
            variables.put("overtimeDate", record.getOvertimeDate() != null ? record.getOvertimeDate().toString() : "未知");
            variables.put("hours", record.getHours() != null ? record.getHours().toString() : "0");
            variables.put("reason", record.getReason());
            variables.put("userName", projectOwner.getName());
            emailService.sendEmail(
                projectOwner.getEmail(),
                "【WBS系统】新加班申请待审批",
                "email/overtime-submitted",
                variables
            );
        }
    }

    public void notifyOvertimeApproved(OvertimeRecord record, User applicant) {
        log.info("Notifying overtime approved for: {}", applicant != null ? applicant.getName() : "unknown");
        if (applicant != null && applicant.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("overtimeDate", record.getOvertimeDate() != null ? record.getOvertimeDate().toString() : "未知");
            variables.put("hours", record.getHours() != null ? record.getHours().toString() : "0");
            variables.put("userName", applicant.getName());
            emailService.sendEmail(
                applicant.getEmail(),
                "【WBS系统】加班申请已通过",
                "email/overtime-approved",
                variables
            );
        }
    }

    public void notifyOvertimeRejected(OvertimeRecord record, User applicant, String rejectReason) {
        log.info("Notifying overtime rejected for: {}", applicant != null ? applicant.getName() : "unknown");
        if (applicant != null && applicant.getEmail() != null) {
            Map<String, Object> variables = new HashMap<>();
            variables.put("overtimeDate", record.getOvertimeDate() != null ? record.getOvertimeDate().toString() : "未知");
            variables.put("hours", record.getHours() != null ? record.getHours().toString() : "0");
            variables.put("rejectReason", rejectReason);
            variables.put("userName", applicant.getName());
            emailService.sendEmail(
                applicant.getEmail(),
                "【WBS系统】加班申请已拒绝",
                "email/overtime-rejected",
                variables
            );
        }
    }

    private String getStatusLabel(String status) {
        return switch (status) {
            case "planning" -> "规划中";
            case "active" -> "进行中";
            case "completed" -> "已完成";
            case "on-hold" -> "已暂停";
            case "cancelled" -> "已取消";
            default -> status;
        };
    }

    private String getTaskStatusLabel(String status) {
        return switch (status) {
            case "todo" -> "待开始";
            case "in-progress" -> "进行中";
            case "done" -> "已完成";
            default -> status;
        };
    }
}
