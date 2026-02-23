package com.wbs.project.mapper;

import com.wbs.project.entity.DelayNotificationRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DelayNotificationRecordMapper {

    int insert(DelayNotificationRecord record);

    DelayNotificationRecord selectByTaskIdAndDate(@Param("taskId") String taskId, 
                                                    @Param("notifiedUserId") String notifiedUserId,
                                                    @Param("notificationDate") LocalDate notificationDate);

    List<DelayNotificationRecord> selectByTaskIdAndDateAfter(@Param("taskId") String taskId,
                                                               @Param("notifiedUserId") String notifiedUserId,
                                                               @Param("notificationDate") LocalDate notificationDate);

    int deleteByDateBefore(@Param("date") LocalDate date);

    int deleteByProjectId(@Param("projectId") String projectId);
}
