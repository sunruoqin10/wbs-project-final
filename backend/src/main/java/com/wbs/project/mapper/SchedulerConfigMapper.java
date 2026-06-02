package com.wbs.project.mapper;

import com.wbs.project.entity.SchedulerConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SchedulerConfigMapper {

    List<SchedulerConfig> selectAll();

    SchedulerConfig selectById(@Param("id") String id);

    int insert(SchedulerConfig config);

    int update(SchedulerConfig config);

    int updateRunTime(@Param("id") String id,
                      @Param("lastRunTime") LocalDateTime lastRunTime,
                      @Param("nextRunTime") LocalDateTime nextRunTime);
}
