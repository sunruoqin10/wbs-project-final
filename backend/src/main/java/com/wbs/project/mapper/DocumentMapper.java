package com.wbs.project.mapper;

import com.wbs.project.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface DocumentMapper {

    List<Document> selectAll();

    Document selectById(@Param("id") String id);

    List<Document> selectByProjectId(@Param("projectId") String projectId);

    List<Document> selectByTaskId(@Param("taskId") String taskId);

    List<Document> selectByCategory(@Param("category") String category);

    List<Document> selectByUserId(@Param("userId") String userId);

    List<Document> selectByProjectIdAndCategory(@Param("projectId") String projectId, @Param("category") String category);

    int insert(Document document);

    int update(Document document);

    int deleteById(@Param("id") String id);

    /**
     * 级联删除：根据项目ID删除该项目下所有文档（含 task 级文档）
     */
    int deleteByProjectId(@Param("projectId") String projectId);

    int incrementDownloadCount(@Param("id") String id);

    int countByProjectId(@Param("projectId") String projectId);

    /**
     * 按"可见上传者"和"可见项目"两个维度取文档并集。
     * 任一参数为 null（admin）或空集合（DEPT_PM 的 projectIds）时跳过对应分支。
     */
    List<Document> selectByAccessibleScope(
        @Param("uploaderIds") Collection<String> uploaderIds,
        @Param("projectIds")  Collection<String> projectIds,
        @Param("category")    String category,
        @Param("reportId")    String reportId,
        @Param("taskId")      String taskId,
        @Param("projectId")   String projectId);

    List<Document> selectByParentId(@Param("parentId") String parentId);

    List<Document> selectByReportId(@Param("reportId") String reportId);

    List<Document> selectByReportIdAndCategory(@Param("reportId") String reportId, @Param("category") String category);
}
