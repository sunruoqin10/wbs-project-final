package com.wbs.project.mapper;

import com.wbs.project.dto.WeeklyReportApprovalLogDTO;
import com.wbs.project.entity.WeeklyReportApprovalLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface WeeklyReportApprovalLogMapper {

    int insert(WeeklyReportApprovalLog row);

    List<WeeklyReportApprovalLogDTO> selectByReportId(@Param("reportId") String reportId);
}
