package com.wbs.project.service;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Document;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
import com.wbs.project.entity.WeeklyReport;
import com.wbs.project.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForeignKeyValidationService {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final UserService userService;
    private final DocumentMapper documentMapper;
    private final WeeklyReportService weeklyReportService;

    public void validateProjectExists(String projectId) {
        if (projectId != null) {
            Project project = projectService.getProjectById(projectId);
            if (project == null) {
                log.warn("项目不存在: projectId={}", projectId);
                throw new IllegalArgumentException("项目不存在");
            }
        }
    }

    public void validateTaskExists(String taskId) {
        if (taskId != null) {
            Task task = taskService.getTaskById(taskId);
            if (task == null) {
                log.warn("任务不存在: taskId={}", taskId);
                throw new IllegalArgumentException("任务不存在");
            }
        }
    }

    public void validateUserExists(String userId) {
        if (userId != null) {
            User user = userService.getUserById(userId);
            if (user == null) {
                log.warn("用户不存在: userId={}", userId);
                throw new IllegalArgumentException("用户不存在");
            }
        }
    }

    public void validateDocumentExists(String documentId) {
        if (documentId != null) {
            Document document = documentMapper.selectById(documentId);
            if (document == null) {
                log.warn("文档不存在: documentId={}", documentId);
                throw new IllegalArgumentException("文档不存在");
            }
        }
    }

    public void validateParentDocumentExists(String parentId) {
        if (parentId != null) {
            Document document = documentMapper.selectById(parentId);
            if (document == null) {
                log.warn("父文档不存在: parentId={}", parentId);
                throw new IllegalArgumentException("父文档不存在");
            }
        }
    }

    public void validateDocumentUpload(String projectId, String taskId, String uploadedBy, String parentId, String reportId) {
        log.debug("验证文档上传参数: projectId={}, taskId={}, uploadedBy={}, parentId={}, reportId={}", projectId, taskId, uploadedBy, parentId, reportId);
        validateProjectExists(projectId);
        validateTaskExists(taskId);
        // Allow anonymous uploads for development - remove this check in production
        if (uploadedBy != null && !uploadedBy.isEmpty()) {
            validateUserExists(uploadedBy);
        } else {
            log.warn("上传用户ID为空，使用默认用户用于开发测试");
            // Don't throw error for development
        }
        validateParentDocumentExists(parentId);
        validateReportExists(reportId);
    }

    public void validateDocumentUpdate(String projectId, String taskId, String parentId) {
        validateProjectExists(projectId);
        validateTaskExists(taskId);
        validateParentDocumentExists(parentId);
    }

    public void validateReportExists(String reportId) {
        if (reportId != null) {
            WeeklyReport report = weeklyReportService.getReportById(reportId);
            if (report == null) {
                log.warn("周报不存在: reportId={}", reportId);
                throw new IllegalArgumentException("周报不存在");
            }
        }
    }
}
