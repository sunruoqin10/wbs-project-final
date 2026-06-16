package com.wbs.project.service;

import com.wbs.project.dto.HeatmapResponse;
import com.wbs.project.entity.PageView;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.mapper.PageViewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PageViewService {

    private final PageViewMapper pageViewMapper;

    private static final Set<String> VALID_WINDOWS =
        Set.of("1d", "7d", "30d", "90d");

    public void record(String userId, String pagePath, String pageName, LocalDateTime occurredAt) {
        PageView pv = new PageView();
        pv.setUserId(userId);
        pv.setPagePath(pagePath);
        pv.setPageName(pageName);
        pv.setOccurredAt(occurredAt);
        pageViewMapper.insert(pv);
    }

    public HeatmapResponse getHeatmap(String window) {
        if (window == null || !VALID_WINDOWS.contains(window)) {
            throw new BusinessException(400, "window 必须是 1d/7d/30d/90d");
        }
        int days = parseDays(window);
        LocalDateTime toTs = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);
        LocalDateTime fromTs = toTs.toLocalDate().minusDays(days - 1L).atStartOfDay();

        List<Map<String, Object>> raw = pageViewMapper.aggregateByPageAndHour(fromTs, toTs);
        List<Map<String, Object>> pageRows = pageViewMapper.aggregateByPage(fromTs, toTs);

        List<String> yAxis = new ArrayList<>();
        Map<String, Long> pageTotal = new LinkedHashMap<>();
        for (Map<String, Object> r : pageRows) {
            String name = (String) r.get("pageName");
            Long total = ((Number) r.get("totalVisits")).longValue();
            yAxis.add(name);
            pageTotal.put(name, total);
        }

        long[][] matrix = new long[yAxis.size()][24];
        Map<String, Integer> pageIdx = new HashMap<>();
        for (int i = 0; i < yAxis.size(); i++) pageIdx.put(yAxis.get(i), i);

        long totalVisits = 0;
        long maxValue = 0L;
        for (Map<String, Object> r : raw) {
            String name = (String) r.get("pageName");
            Integer hour = ((Number) r.get("hour")).intValue();
            Long pv = ((Number) r.get("pv")).longValue();
            Integer yi = pageIdx.get(name);
            if (yi == null) continue;
            matrix[yi][hour] = pv;
            totalVisits += pv;
            if (pv > maxValue) maxValue = pv;
        }

        HeatmapResponse.Kpi kpi = new HeatmapResponse.Kpi();
        kpi.setTotalVisits(totalVisits);

        if (!yAxis.isEmpty()) {
            String topPage = yAxis.get(0);
            kpi.setTopPageName(topPage);
            kpi.setTopPageTotal(pageTotal.get(topPage));
        }

        int peakHour = -1;
        long peakHourTotal = 0L;
        for (int h = 0; h < 24; h++) {
            long sum = 0;
            for (int y = 0; y < yAxis.size(); y++) sum += matrix[y][h];
            if (sum > peakHourTotal) {
                peakHourTotal = sum;
                peakHour = h;
            }
        }
        kpi.setPeakHour(peakHour >= 0 ? peakHour : 0);
        kpi.setPeakHourTotal(peakHourTotal);

        HeatmapResponse resp = new HeatmapResponse();
        resp.setWindow(window);
        resp.setFromTs(fromTs.toString());
        resp.setToTs(toTs.toString());
        resp.setDays(days);
        resp.setKpi(kpi);
        resp.setXAxis(List.of(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23));
        resp.setYAxis(yAxis);
        List<List<Long>> matrixOut = new ArrayList<>();
        for (long[] row : matrix) {
            List<Long> r = new ArrayList<>(24);
            for (long v : row) r.add(v);
            matrixOut.add(r);
        }
        resp.setMatrix(matrixOut);
        resp.setMaxValue(maxValue);
        return resp;
    }

    private static int parseDays(String window) {
        return switch (window) {
            case "1d" -> 1;
            case "7d" -> 7;
            case "30d" -> 30;
            case "90d" -> 90;
            default -> throw new BusinessException(400, "window 非法");
        };
    }
}
