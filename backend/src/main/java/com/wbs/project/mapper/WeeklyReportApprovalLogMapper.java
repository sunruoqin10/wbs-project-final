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

    /**
     * 级联删除：根据周报ID删除该周报所有审批日志
     */
    int deleteByReportId(@Param("reportId") String reportId);
}
