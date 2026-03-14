package com.wbs.project.mapper;

import com.wbs.project.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper {

    List<Document> selectAll();

    Document selectById(@Param("id") String id);

    List<Document> selectByProjectId(@Param("projectId") String projectId);

    List<Document> selectByTaskId(@Param("taskId") String taskId);

    List<Document> selectByCategory(@Param("category") String category);

    List<Document> selectByProjectIdAndCategory(@Param("projectId") String projectId, @Param("category") String category);

    int insert(Document document);

    int update(Document document);

    int deleteById(@Param("id") String id);

    int incrementDownloadCount(@Param("id") String id);

    int countByProjectId(@Param("projectId") String projectId);

    List<Document> selectByParentId(@Param("parentId") String parentId);
}
