package com.wbs.project.dto;
import lombok.Data;
import java.util.List;

@Data
public class HeatmapResponse {
    private String window;
    private String fromTs;
    private String toTs;
    private Integer days;
    private Kpi kpi;
    private List<Integer> xAxis;
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
