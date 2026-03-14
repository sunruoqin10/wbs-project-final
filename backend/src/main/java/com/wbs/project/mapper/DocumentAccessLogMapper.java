package com.wbs.project.mapper;

import com.wbs.project.entity.DocumentAccessLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentAccessLogMapper {

    List<DocumentAccessLog> selectByDocumentId(@Param("documentId") String documentId);

    List<DocumentAccessLog> selectByUserId(@Param("userId") String userId);

    List<DocumentAccessLog> selectByDocumentIdAndAction(@Param("documentId") String documentId, @Param("action") String action);

    List<DocumentAccessLog> selectByUserIdAndAction(@Param("userId") String userId, @Param("action") String action);

    int insert(DocumentAccessLog log);

    int deleteByDocumentId(@Param("documentId") String documentId);
}
