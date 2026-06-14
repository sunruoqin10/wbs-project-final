package com.wbs.project.mapper;

import com.wbs.project.entity.OvertimeApprovalLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 加班审批日志 Mapper
 *
 * 2026-06-14: 新建 — 对应 t_overtime_approval_log 表
 */
@Mapper
public interface OvertimeApprovalLogMapper {

    /**
     * 插入一条审批日志
     */
    int insert(OvertimeApprovalLog log);

    /**
     * 查询某条加班记录的所有审批日志(按时间倒序)
     */
    List<OvertimeApprovalLog> selectByOvertimeId(@Param("overtimeId") String overtimeId);
}