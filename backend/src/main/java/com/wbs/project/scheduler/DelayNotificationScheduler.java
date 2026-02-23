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
                taskService.updateDelayedStatus(allTasks);

                List<Task> tasksToNotify = new ArrayList<>();
                for (Task task : allTasks) {
                    if (task.getIsDelayed() == null || !task.getIsDelayed()) {
                        continue;
                    }
                    if ("done".equals(task.getStatus())) {
                        continue;
                    }

                    boolean isLeaf = taskService.isLeafTask(task.getId());
                    if (isLeaf) {
                        tasksToNotify.add(task);
                    } else {
                        List<Task> descendants = taskService.getAllDescendantTasks(task.getId());
                        boolean hasDelayedLeafDescendant = false;
                        for (Task descendant : descendants) {
                            if (taskService.isLeafTask(descendant.getId()) && 
                                descendant.getIsDelayed() != null && 
                                descendant.getIsDelayed()) {
                                hasDelayedLeafDescendant = true;
                                break;
                            }
                        }
                        if (!hasDelayedLeafDescendant) {
                            tasksToNotify.add(task);
                        }
                    }
                }

                if (!tasksToNotify.isEmpty()) {
                    User projectOwner = project.getOwnerId() != null ? 
                        userService.getUserById(project.getOwnerId()) : null;
                    List<User> managers = userService.getManagers();

                    for (Task task : tasksToNotify) {
                        User assignee = task.getAssigneeId() != null ? 
                            userService.getUserById(task.getAssigneeId()) : null;

                        if (assignee != null && assignee.getEmail() != null) {
                            List<DelayNotificationRecord> existingRecords = 
                                delayNotificationRecordMapper.selectByTaskIdAndDateAfter(
                                    task.getId(), assignee.getId(), yesterday);
                            
                            if (existingRecords == null || existingRecords.isEmpty()) {
                                emailNotificationService.notifyTaskDelayed(task, assignee, projectOwner);
                                recordNotification(task, project.getId(), assignee, today);
                                totalNotified++;
                            }
                        }

                        if (projectOwner != null && projectOwner.getEmail() != null && 
                            (assignee == null || !projectOwner.getId().equals(assignee.getId()))) {
                            List<DelayNotificationRecord> existingRecords = 
                                delayNotificationRecordMapper.selectByTaskIdAndDateAfter(
                                    task.getId(), projectOwner.getId(), yesterday);
                            
                            if (existingRecords == null || existingRecords.isEmpty()) {
                                emailNotificationService.notifyTaskDelayed(task, assignee, projectOwner);
                                recordNotification(task, project.getId(), projectOwner, today);
                                totalNotified++;
                            }
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
