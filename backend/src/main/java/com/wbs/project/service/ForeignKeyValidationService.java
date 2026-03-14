package com.wbs.project.service;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Document;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.Task;
import com.wbs.project.entity.User;
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

    public void validateDocumentUpload(String projectId, String taskId, String uploadedBy, String parentId) {
        log.debug("验证文档上传参数: projectId={}, taskId={}, uploadedBy={}, parentId={}", projectId, taskId, uploadedBy, parentId);
        validateProjectExists(projectId);
        validateTaskExists(taskId);
        if (uploadedBy == null || uploadedBy.isEmpty()) {
            log.warn("上传用户ID为空");
            throw new IllegalArgumentException("上传用户ID不能为空");
        }
        validateUserExists(uploadedBy);
        validateParentDocumentExists(parentId);
    }

    public void validateDocumentUpdate(String projectId, String taskId, String parentId) {
        validateProjectExists(projectId);
        validateTaskExists(taskId);
        validateParentDocumentExists(parentId);
    }
}
