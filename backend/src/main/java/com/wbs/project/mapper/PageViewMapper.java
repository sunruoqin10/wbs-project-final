package com.wbs.project.mapper;

import com.wbs.project.entity.PageView;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface PageViewMapper {
    int insert(PageView record);

    /** 热力图主查询:按 mode 选分桶维度,返回 [page_name, bucket, pv]
     *  mode: hour | dayOfWeek | date | week */
    List<Map<String, Object>> aggregateByBucket(
        @Param("fromTs") LocalDateTime fromTs,
        @Param("toTs")   LocalDateTime toTs,
        @Param("mode")   String mode
    );

    /** 页面列表 + 各页面窗口内总 PV + 日均 */
    List<Map<String, Object>> aggregateByPage(
        @Param("fromTs") LocalDateTime fromTs,
        @Param("toTs")   LocalDateTime toTs
    );

    /** 清理超期日志(后续 PR 用,本期不挂 scheduler) */
    int deleteOlderThan(@Param("cutoff") LocalDateTime cutoff);
}
