package com.wbs.project.mapper;

import com.wbs.project.entity.OvertimeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 加班记录Mapper接口
 */
@Mapper
public interface OvertimeMapper {

    /**
     * 查询所有加班记录
     */
    List<OvertimeRecord> selectAll();

    /**
     * 根据ID查询加班记录
     */
    OvertimeRecord selectById(@Param("id") String id);

    /**
     * 根据用户ID查询加班记录
     */
    List<OvertimeRecord> selectByUserId(@Param("userId") String userId);

    /**
     * 根据项目ID查询加班记录
     */
    List<OvertimeRecord> selectByProjectId(@Param("projectId") String projectId);

    /**
     * 根据状态查询加班记录
     */
    List<OvertimeRecord> selectByStatus(@Param("status") String status);

    /**
     * 根据日期范围查询加班记录
     */
    List<OvertimeRecord> selectByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据用户ID和日期范围查询加班记录
     */
    List<OvertimeRecord> selectByUserIdAndDateRange(@Param("userId") String userId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据项目ID和日期范围查询加班记录
     */
    List<OvertimeRecord> selectByProjectIdAndDateRange(@Param("projectId") String projectId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * 根据条件查询加班记录（支持多条件组合）
     */
    List<OvertimeRecord> selectByCondition(@Param("userId") String userId, 
                                           @Param("projectId") String projectId, 
                                           @Param("status") String status,
                                           @Param("startDate") LocalDate startDate, 
                                           @Param("endDate") LocalDate endDate,
                                           @Param("overtimeType") String overtimeType);

    /**
     * 插入加班记录
     */
    int insert(OvertimeRecord record);

    /**
     * 更新加班记录
     */
    int update(OvertimeRecord record);

    /**
     * 删除加班记录
     */
    int deleteById(@Param("id") String id);

    /**
     * 统计加班记录总数
     */
    int countTotal();

    /**
     * 统计指定用户的加班记录数
     */
    int countByUserId(@Param("userId") String userId);

    /**
     * 统计指定项目的加班记录数
     */
    int countByProjectId(@Param("projectId") String projectId);

    /**
     * 统计指定状态的加班记录数
     */
    int countByStatus(@Param("status") String status);

    /**
     * 统计指定用户的总加班时长
     */
    BigDecimal sumHoursByUserId(@Param("userId") String userId);

    /**
     * 统计指定项目的总加班时长
     */
    BigDecimal sumHoursByProjectId(@Param("projectId") String projectId);

    /**
     * 按用户统计加班时长
     */
    List<Map<String, Object>> sumHoursGroupByUser(@Param("projectId") String projectId, 
                                                   @Param("startDate") LocalDate startDate, 
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 按项目统计加班时长
     */
    List<Map<String, Object>> sumHoursGroupByProject(@Param("userId") String userId, 
                                                      @Param("startDate") LocalDate startDate, 
                                                      @Param("endDate") LocalDate endDate);

    /**
     * 按日期统计加班时长
     */
    List<Map<String, Object>> sumHoursGroupByDate(@Param("userId") String userId,
                                                   @Param("projectId") String projectId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 按加班类型统计加班时长
     */
    List<Map<String, Object>> sumHoursGroupByType(@Param("userId") String userId,
                                                   @Param("projectId") String projectId,
                                                   @Param("startDate") LocalDate startDate,
                                                   @Param("endDate") LocalDate endDate);

    /**
     * 统计待审批的加班记录数
     */
    int countPendingByProjectId(@Param("projectId") String projectId);
}
