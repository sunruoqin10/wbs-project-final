package com.wbs.project.mapper;

import com.wbs.project.entity.ProjectHandoverLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface ProjectHandoverLogMapper {
    int insert(ProjectHandoverLog log);

    /** 用户维度交接历史(spec §4.4.2) */
    List<ProjectHandoverLog> selectByUserId(
        @Param("userId") String userId,
        @Param("handoverType") String handoverType,
        @Param("offset") int offset,
        @Param("limit") int limit);

    int countByUserId(@Param("userId") String userId,
                      @Param("handoverType") String handoverType);
}