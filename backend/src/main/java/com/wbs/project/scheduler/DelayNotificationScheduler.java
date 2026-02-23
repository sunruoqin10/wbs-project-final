package com.wbs.project.scheduler;

import com.wbs.project.entity.DelayNotificationRecord;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.mapper.DelayNotificationRecordMapper;
import com.wbs.project.service.EmailNotificationService;
import com.wbs.project.service.ProjectService;
import com.wbs.project.service.TaskService;
import com.wbs.project.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class DelayNotificationScheduler {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final UserService userService;
    private final EmailNotificationService emailNotificationService;
    private final DelayNotificationRecordMapper delayNotificationRecordMapper;

    @Scheduled(cron = "0 0 9 * * ?")
    public void checkAndSendDelayNotifications() {
        log.info("========== 开始检查延期任务 ==========");
        try {
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);

            List<Project> projects = projectService.getAllProjects();
            int totalNotified = 0;

            for (Project project : projects) {
                if (!"active".equals(project.getStatus()) && !"planning".equals(project.getStatus())) {
                    continue;
                }

                List<Task> allTasks = taskService.getTasksByProjectId(project.getId());
                log.info("项目 [{}] 共有 {} 个任务", project.getName(), allTasks.size());
                
                taskService.updateDelayedStatus(allTasks);

                List<Task> tasksToNotify = new ArrayList<>();
                
                boolean onlyOneRootTask = allTasks.size() == 1 && allTasks.get(0).getParentTaskId() == null;
                
                for (Task task : allTasks) {
                    log.info("  检查任务: {} | 状态: {} | 延期: {} | 结束日期: {} | 是叶子: {}", 
                        task.getTitle(), 
                        task.getStatus(), 
                        task.getIsDelayed(),
                        task.getEndDate(),
                        taskService.isLeafTask(task.getId()));
                    
                    if (task.getIsDelayed() == null || !task.getIsDelayed()) {
                        log.info("    → 跳过: 未延期");
                        continue;
                    }
                    if ("done".equals(task.getStatus())) {
                        log.info("    → 跳过: 已完成");
                        continue;
                    }

                    boolean isLeaf = taskService.isLeafTask(task.getId());
                    if (isLeaf) {
                        log.info("    → 加入通知列表: 叶子任务");
                        tasksToNotify.add(task);
                    } else if (onlyOneRootTask) {
                        log.info("    → 加入通知列表: 单个根任务");
                        tasksToNotify.add(task);
                    } else {
                        log.info("    → 跳过: 非叶子且非单个根任务");
                    }
                }
                
                log.info("项目 [{}] 需要通知的任务数: {}", project.getName(), tasksToNotify.size());

                if (!tasksToNotify.isEmpty()) {
                    User projectOwner = project.getOwnerId() != null ? 
                        userService.getUserById(project.getOwnerId()) : null;
                    List<User> managers = userService.getManagers();

                    for (Task task : tasksToNotify) {
                        log.info("  准备发送邮件给任务: {}", task.getTitle());
                        
                        User assignee = task.getAssigneeId() != null ? 
                            userService.getUserById(task.getAssigneeId()) : null;
                        
                        log.info("    任务负责人: {} | 邮箱: {}", 
                            assignee != null ? assignee.getName() : "无",
                            assignee != null ? assignee.getEmail() : "无");
                        
                        log.info("    项目负责人: {} | 邮箱: {}", 
                            projectOwner != null ? projectOwner.getName() : "无",
                            projectOwner != null ? projectOwner.getEmail() : "无");

                        boolean assigneeNotified = false;
                        boolean ownerNotified = false;

                        if (assignee != null && assignee.getEmail() != null) {
                            List<DelayNotificationRecord> existingRecords = 
                                delayNotificationRecordMapper.selectByTaskIdAndDateAfter(
                                    task.getId(), assignee.getId(), yesterday);
                            
                            if (existingRecords == null || existingRecords.isEmpty()) {
                                log.info("    → 任务负责人需要发送邮件（24小时内未发送过）");
                                assigneeNotified = true;
                            } else {
                                log.info("    → 任务负责人跳过（24小时内已发送过）");
                            }
                        } else {
                            log.info("    → 任务负责人跳过（无邮箱）");
                        }

                        if (projectOwner != null && projectOwner.getEmail() != null && 
                            (assignee == null || !projectOwner.getId().equals(assignee.getId()))) {
                            List<DelayNotificationRecord> existingRecords = 
                                delayNotificationRecordMapper.selectByTaskIdAndDateAfter(
                                    task.getId(), projectOwner.getId(), yesterday);
                            
                            if (existingRecords == null || existingRecords.isEmpty()) {
                                log.info("    → 项目负责人需要发送邮件（24小时内未发送过）");
                                ownerNotified = true;
                            } else {
                                log.info("    → 项目负责人跳过（24小时内已发送过）");
                            }
                        } else {
                            log.info("    → 项目负责人跳过（无邮箱或与任务负责人相同）");
                        }

                        if (assigneeNotified || ownerNotified) {
                            log.info("    → 执行发送邮件...");
                            emailNotificationService.notifyTaskDelayed(task, assignee, projectOwner);
                            
                            if (assigneeNotified) {
                                recordNotification(task, project.getId(), assignee, today);
                                totalNotified++;
                                log.info("    ✓ 已记录任务负责人通知");
                            }
                            if (ownerNotified) {
                                recordNotification(task, project.getId(), projectOwner, today);
                                totalNotified++;
                                log.info("    ✓ 已记录项目负责人通知");
                            }
                        } else {
                            log.info("    → 无需发送邮件给任何人");
                        }

                        for (User manager : managers) {
                            if (manager.getEmail() == null) {
                                continue;
                            }
                            if (assignee != null && manager.getId().equals(assignee.getId())) {
                                continue;
                            }
                            if (projectOwner != null && manager.getId().equals(projectOwner.getId())) {
                                continue;
                            }

                            List<DelayNotificationRecord> existingRecords = 
                                delayNotificationRecordMapper.selectByTaskIdAndDateAfter(
                                    task.getId(), manager.getId(), yesterday);
                            
                            if (existingRecords == null || existingRecords.isEmpty()) {
                                emailNotificationService.notifyTaskDelayedToManager(task, assignee, manager);
                                recordNotification(task, project.getId(), manager, today);
                                totalNotified++;
                            }
                        }
                    }
                }
            }

            log.info("========== 延期检查完成，共发送 {} 封提醒邮件 ==========", totalNotified);
        } catch (Exception e) {
            log.error("延期检查过程中发生错误", e);
        }
    }

    private void recordNotification(Task task, String projectId, User user, LocalDate date) {
        DelayNotificationRecord record = new DelayNotificationRecord();
        record.setId("dnr" + UUID.randomUUID().toString().substring(0, 8));
        record.setTaskId(task.getId());
        record.setProjectId(projectId);
        record.setNotifiedUserId(user.getId());
        record.setNotifiedEmail(user.getEmail());
        record.setNotificationDate(date);
        record.setCreatedAt(LocalDateTime.now());
        delayNotificationRecordMapper.insert(record);
    }

    public void checkAndSendDelayNotificationsManual() {
        checkAndSendDelayNotifications();
    }
}
