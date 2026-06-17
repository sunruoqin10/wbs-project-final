package com.wbs.project.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class HeatmapResponse {
    private String window;
    private String fromTs;
    private String toTs;
    private Integer days;
    private Kpi kpi;

    // 显式钉死 JSON 字段名,避免被全局 PropertyNamingStrategy 改成小写
    // (前端 AccessHeatmapView.vue 读 d.xAxis / d.yAxis,一旦变成 xaxis/yAxis
    //  会导致 yAxis.data=[] → ECharts heatmap Y 轴不显示 → 整张图看不见)
    @JsonProperty("xAxis")
    private List<String> xAxis;
    @JsonProperty("xAxisUnit")
    private String xAxisUnit; // hour | dayOfWeek | date | week
    @JsonProperty("yAxis")
    private List<String> yAxis;

    private List<List<Long>> matrix;
    private Long maxValue;

    @Data
    public static class Kpi {
        private Long totalVisits;
        private String topPageName;
        private Long topPageTotal;
        private Integer peakHour;
        private Long peakHourTotal;
    }
}
