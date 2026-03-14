package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.Document;
import com.wbs.project.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public Result<List<Document>> getAllDocuments() {
        List<Document> documents = documentService.getAllDocuments();
        return Result.success(documents);
    }

    @GetMapping("/{id}")
    public Result<Document> getDocumentById(@PathVariable String id) {
        Document document = documentService.getDocumentById(id);
        if (document == null) {
            return Result.error("文档不存在");
        }
        return Result.success(document);
    }

    @GetMapping("/project/{projectId}")
    public Result<List<Document>> getDocumentsByProjectId(@PathVariable String projectId) {
        List<Document> documents = documentService.getDocumentsByProjectId(projectId);
        return Result.success(documents);
    }

    @GetMapping("/task/{taskId}")
    public Result<List<Document>> getDocumentsByTaskId(@PathVariable String taskId) {
        List<Document> documents = documentService.getDocumentsByTaskId(taskId);
        return Result.success(documents);
    }

    @GetMapping("/category/{category}")
    public Result<List<Document>> getDocumentsByCategory(@PathVariable String category) {
        List<Document> documents = documentService.getDocumentsByCategory(category);
        return Result.success(documents);
    }

    @GetMapping("/report/{reportId}")
    public Result<List<Document>> getDocumentsByReportId(@PathVariable String reportId) {
        List<Document> documents = documentService.getDocumentsByReportId(reportId);
        return Result.success(documents);
    }

    @GetMapping("/report/{reportId}/category/{category}")
    public Result<List<Document>> getDocumentsByReportIdAndCategory(
            @PathVariable String reportId,
            @PathVariable String category) {
        List<Document> documents = documentService.getDocumentsByReportIdAndCategory(reportId, category);
        return Result.success(documents);
    }

    @GetMapping("/user/{userId}")
    public Result<List<Document>> getDocumentsByUserId(@PathVariable String userId) {
        List<Document> documents = documentService.getDocumentsByUserId(userId);
        return Result.success(documents);
    }

    @GetMapping("/report/{reportId}/stats")
    public Result<Map<String, Object>> getReportDocumentStats(@PathVariable String reportId) {
        Map<String, Object> stats = documentService.getReportDocumentStats(reportId);
        return Result.success(stats);
    }

    @PostMapping("/upload")
    public Result<Document> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam("category") String category,
            @RequestParam(value = "projectId", required = false) String projectId,
            @RequestParam(value = "taskId", required = false) String taskId,
            @RequestParam(value = "parentId", required = false) String parentId,
            @RequestParam(value = "reportId", required = false) String reportId,
            @RequestParam(value = "description", required = false) String description,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        log.info("文档上传请求: file={}, name={}, category={}, projectId={}, taskId={}, reportId={}, userId={}",
                file.getOriginalFilename(), name, category, projectId, taskId, reportId, userId);
        try {
            Document document = documentService.uploadDocument(file, name, category, projectId, taskId, parentId, reportId, description, userId);
            log.info("文档上传成功: documentId={}, fileName={}", document.getId(), document.getFileName());
            return Result.success("文档上传成功", document);
        } catch (IllegalArgumentException e) {
            log.warn("文档上传失败(参数验证): {}", e.getMessage(), e);
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文档上传异常: ", e);
            log.error("异常类型: {}, 消息: {}", e.getClass().getSimpleName(), e.getMessage());
            return Result.error("文档上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileContent = documentService.downloadDocument(id, userId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(document.getFileType()));
            headers.setContentDispositionFormData("attachment", document.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (IllegalArgumentException e) {
            log.warn("文档下载失败: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("文档下载异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewDocument(@PathVariable String id) {
        try {
            Document document = documentService.getDocumentById(id);
            if (document == null) {
                return ResponseEntity.notFound().build();
            }

            String fileType = document.getFileType().toLowerCase();
            if (!fileType.startsWith("image/") && !fileType.equals("application/pdf")) {
                HttpHeaders errorHeaders = new HttpHeaders();
                errorHeaders.setContentType(MediaType.APPLICATION_JSON);
                String errorMessage = "{\"code\":400,\"message\":\"此文件类型不支持在线预览\",\"data\":null}";
                try {
                    return ResponseEntity.badRequest()
                            .headers(errorHeaders)
                            .body(errorMessage.getBytes("UTF-8"));
                } catch (Exception e) {
                    log.error("编码错误消息失败", e);
                    return ResponseEntity.badRequest().build();
                }
            }

            byte[] fileContent = documentService.downloadDocument(id, null);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(document.getFileType()));
            headers.setContentDispositionFormData("inline", document.getFileName());

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(fileContent);
        } catch (Exception e) {
            log.error("文档预览异常", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{id}")
    public Result<Document> updateDocument(
            @PathVariable String id,
            @RequestBody Document document,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            Document updatedDocument = documentService.updateDocument(
                    id,
                    document.getName(),
                    document.getCategory(),
                    document.getProjectId(),
                    document.getTaskId(),
                    document.getParentId(),
                    document.getDescription()
            );
            return Result.success("文档更新成功", updatedDocument);
        } catch (IllegalArgumentException e) {
            log.warn("文档更新失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文档更新异常", e);
            return Result.error("文档更新失败");
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteDocument(
            @PathVariable String id,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        try {
            documentService.deleteDocument(id, userId);
            return Result.success();
        } catch (IllegalArgumentException e) {
            log.warn("文档删除失败: {}", e.getMessage());
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("文档删除异常", e);
            return Result.error("文档删除失败");
        }
    }

    @GetMapping("/project/{projectId}/count")
    public Result<Integer> getDocumentCountByProjectId(@PathVariable String projectId) {
        int count = documentService.getDocumentCountByProjectId(projectId);
        return Result.success(count);
    }
}
