package com.wbs.project.mapper;

import com.wbs.project.entity.EmailLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface EmailLogMapper {

    int insert(EmailLog emailLog);

    EmailLog selectById(@Param("id") String id);

    List<EmailLog> selectByToEmail(@Param("toEmail") String toEmail);

    List<EmailLog> selectByStatus(@Param("status") String status);

    List<EmailLog> selectByDateRange(@Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);

    int deleteByDateBefore(@Param("date") LocalDateTime date);
}
