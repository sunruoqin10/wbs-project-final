package com.wbs.project.mapper;

import com.wbs.project.entity.WeeklyReportComment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeeklyReportCommentMapper {

    List<WeeklyReportComment> selectByReportId(@Param("reportId") String reportId);

    WeeklyReportComment selectById(@Param("id") String id);

    int insert(WeeklyReportComment comment);

    int deleteById(@Param("id") String id);

    int deleteByReportId(@Param("reportId") String reportId);

    int countByReportId(@Param("reportId") String reportId);
}
