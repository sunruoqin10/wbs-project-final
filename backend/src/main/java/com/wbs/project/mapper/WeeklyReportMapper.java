package com.wbs.project.mapper;

import com.wbs.project.entity.WeeklyReport;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface WeeklyReportMapper {

    List<WeeklyReport> selectAll();

    WeeklyReport selectById(@Param("id") String id);

    List<WeeklyReport> selectByUserId(@Param("userId") String userId);

    List<WeeklyReport> selectByProjectId(@Param("projectId") String projectId);

    List<WeeklyReport> selectByWeekRange(@Param("weekStart") LocalDate weekStart, @Param("weekEnd") LocalDate weekEnd);

    WeeklyReport selectByUserAndWeek(@Param("userId") String userId, @Param("weekStart") LocalDate weekStart);

    int insert(WeeklyReport report);

    int update(WeeklyReport report);

    int deleteById(@Param("id") String id);

    int countByUserId(@Param("userId") String userId);

    int countByProjectId(@Param("projectId") String projectId);

    int countByStatus(@Param("status") String status);
}
