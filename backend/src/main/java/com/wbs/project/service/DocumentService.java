package com.wbs.project.service;

import com.wbs.project.entity.Document;
import com.wbs.project.entity.DocumentAccessLog;
import com.wbs.project.mapper.DocumentAccessLogMapper;
import com.wbs.project.mapper.DocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentMapper documentMapper;
    private final DocumentAccessLogMapper documentAccessLogMapper;
    private final ForeignKeyValidationService foreignKeyValidationService;

    @Value("${document.upload.path:./uploads/documents}")
    private String uploadPath;

    @Value("${document.upload.max-size:10485760}")
    private long maxFileSize;

    public List<Document> getAllDocuments() {
        return documentMapper.selectAll();
    }

    public Document getDocumentById(String id) {
        return documentMapper.selectById(id);
    }

    public List<Document> getDocumentsByProjectId(String projectId) {
        return documentMapper.selectByProjectId(projectId);
    }

    public List<Document> getDocumentsByTaskId(String taskId) {
        return documentMapper.selectByTaskId(taskId);
    }

    public List<Document> getDocumentsByCategory(String category) {
        return documentMapper.selectByCategory(category);
    }

    public List<Document> getDocumentsByUserId(String userId) {
        return documentMapper.selectByUserId(userId);
    }

    public List<Document> getDocumentsByProjectIdAndCategory(String projectId, String category) {
        return documentMapper.selectByProjectIdAndCategory(projectId, category);
    }

    public List<Document> getDocumentsByReportId(String reportId) {
        return documentMapper.selectByReportId(reportId);
    }

    public List<Document> getDocumentsByReportIdAndCategory(String reportId, String category) {
        return documentMapper.selectByReportIdAndCategory(reportId, category);
    }

    public Map<String, Object> getReportDocumentStats(String reportId) {
        List<Document> documents = documentMapper.selectByReportId(reportId);

        Map<String, Integer> categoryCount = new LinkedHashMap<>();
        categoryCount.put("requirements", 0);
        categoryCount.put("design", 0);
        categoryCount.put("development", 0);
        categoryCount.put("testing", 0);
        categoryCount.put("deployment", 0);
        categoryCount.put("documentation", 0);
        categoryCount.put("other", 0);

        long totalSize = 0;
        for (Document doc : documents) {
            String cat = doc.getCategory() != null ? doc.getCategory() : "other";
            categoryCount.put(cat, categoryCount.getOrDefault(cat, 0) + 1);
            totalSize += doc.getFileSize();
        }

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", documents.size());
        stats.put("categories", categoryCount);
        stats.put("totalSize", totalSize);

        return stats;
    }

    @Transactional
    public Document uploadDocument(MultipartFile file, String name, String category,
                                String projectId, String taskId, String parentId, String reportId,
                                String description, String uploadedBy) {
        log.info("开始上传文档: fileName={}, category='{}', projectId={}, taskId={}, reportId={}, uploadedBy={}",
                file.getOriginalFilename(), category, projectId, taskId, reportId, uploadedBy);

        validateFile(file);
        foreignKeyValidationService.validateDocumentUpload(projectId, taskId, uploadedBy, parentId, reportId);

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String fileName = (name != null && !name.isEmpty()) ? name : originalFilename;

        String documentId = UUID.randomUUID().toString();
        String filePath = saveFile(file, documentId, fileExtension, reportId, category);

        int version = calculateVersion(projectId, category);

        Document document = new Document();
        document.setId(documentId);
        document.setProjectId(projectId);
        document.setTaskId(taskId);
        document.setName(fileName);
        document.setCategory(category);
        document.setFileName(originalFilename);
        document.setFilePath(filePath);
        document.setFileSize(file.getSize());
        document.setFileType(file.getContentType());
        document.setFileExtension(fileExtension);
        document.setVersion(version);
        document.setParentId(parentId);
        document.setReportId(reportId);
        document.setDescription(description);
        document.setUploadedBy(uploadedBy != null && !uploadedBy.isEmpty() ? uploadedBy : "system-default");
        document.setStatus("active");
        document.setDownloadCount(0);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());

        documentMapper.insert(document);
        logAccess(document.getId(), uploadedBy, "upload", null);

        log.info("文档上传成功: id={}, name={}, uploadedBy={}", document.getId(), fileName, uploadedBy);
        return document;
    }

    @Transactional
    public byte[] downloadDocument(String id, String userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!"active".equals(document.getStatus())) {
            throw new IllegalArgumentException("文档已删除");
        }

        documentMapper.incrementDownloadCount(id);
        logAccess(id, userId, "download", null);

        try {
            Path path = Paths.get(document.getFilePath());
            return Files.readAllBytes(path);
        } catch (IOException e) {
            log.error("读取文件失败: path={}", document.getFilePath(), e);
            throw new RuntimeException("读取文件失败");
        }
    }

    @Transactional
    public Document updateDocument(String id, String name, String category,
                               String projectId, String taskId, String parentId,
                               String description) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!"active".equals(document.getStatus())) {
            throw new IllegalArgumentException("文档已删除");
        }

        foreignKeyValidationService.validateDocumentUpdate(projectId, taskId, parentId);

        if (name != null && !name.isEmpty()) {
            document.setName(name);
        }
        if (category != null && !category.isEmpty()) {
            document.setCategory(category);
        }
        document.setProjectId(projectId);
        document.setTaskId(taskId);
        document.setParentId(parentId);
        document.setDescription(description);

        documentMapper.update(document);
        logAccess(id, document.getUploadedBy(), "update", null);

        log.info("文档更新成功: id={}, name={}", id, name);
        return document;
    }

    @Transactional
    public void deleteDocument(String id, String userId) {
        Document document = documentMapper.selectById(id);
        if (document == null) {
            throw new IllegalArgumentException("文档不存在");
        }
        if (!"active".equals(document.getStatus())) {
            throw new IllegalArgumentException("文档已删除");
        }

        documentMapper.deleteById(id);
        documentAccessLogMapper.deleteByDocumentId(id);

        List<Document> childDocuments = documentMapper.selectByParentId(id);
        for (Document child : childDocuments) {
            child.setParentId(null);
            documentMapper.update(child);
        }

        logAccess(id, userId, "delete", null);
        log.info("文档删除成功: id={}, deletedBy={}", id, userId);
    }

    public int getDocumentCountByProjectId(String projectId) {
        return documentMapper.countByProjectId(projectId);
    }

    private void logAccess(String documentId, String userId, String action, String ipAddress) {
        DocumentAccessLog log = new DocumentAccessLog();
        log.setId(UUID.randomUUID().toString());
        log.setDocumentId(documentId);
        log.setUserId(userId);
        log.setAction(action);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(LocalDateTime.now());
        documentAccessLogMapper.insert(log);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex == -1) ? "" : filename.substring(dotIndex + 1).toLowerCase();
    }

    private String saveFile(MultipartFile file, String fileId, String extension, String reportId, String category) {
        try {
            Path uploadDir;

            if (reportId != null && !reportId.isEmpty()) {
                String categoryFolder = (category != null && !category.isEmpty()) ? category : "other";
                uploadDir = Paths.get(uploadPath, "reports", reportId, categoryFolder);
            } else {
                uploadDir = Paths.get(uploadPath, "general", category != null ? category : "other");
            }

            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String fileName = fileId + "." + extension;
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            log.info("文件保存成功: path={}", filePath);
            return filePath.toString();
        } catch (IOException e) {
            log.error("文件保存失败: fileName={}", file.getOriginalFilename(), e);
            throw new RuntimeException("文件保存失败");
        }
    }

    private int calculateVersion(String projectId, String category) {
        List<Document> documents = documentMapper.selectByProjectIdAndCategory(projectId, category);
        return documents.size() + 1;
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new IllegalArgumentException("文件大小超过限制");
        }

        String contentType = file.getContentType();
        if (!isAllowedFileType(contentType)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
    }

    private boolean isAllowedFileType(String contentType) {
        if (contentType == null) {
            return false;
        }
        String lowerContentType = contentType.toLowerCase();
        return lowerContentType.startsWith("image/") ||
                lowerContentType.equals("application/pdf") ||
                lowerContentType.contains("word") ||
                lowerContentType.contains("excel") ||
                lowerContentType.contains("powerpoint") ||
                lowerContentType.contains("msword") ||
                lowerContentType.contains("msexcel") ||
                lowerContentType.contains("mspowerpoint") ||
                lowerContentType.contains("officedocument") ||
                lowerContentType.contains("officespreadsheet") ||
                lowerContentType.contains("officepresentation");
    }
}
