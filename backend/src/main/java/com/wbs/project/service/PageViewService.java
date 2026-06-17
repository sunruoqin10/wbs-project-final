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
import java.time.temporal.WeekFields;
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
        LocalDate today = LocalDate.now();
        LocalDateTime toTs = LocalDateTime.of(today, LocalTime.MAX);
        LocalDateTime fromTs = today.minusDays(days - 1L).atStartOfDay();

        // 1) 根据 window 决定 X 轴分桶
        XBucketPlan plan = buildBuckets(window, today);
        List<String> xBuckets = plan.buckets;
        String unit = plan.unit;
        String mode = plan.mode;

        // 2) 拉数据
        List<Map<String, Object>> raw = pageViewMapper.aggregateByBucket(fromTs, toTs, mode);
        List<Map<String, Object>> pageRows = pageViewMapper.aggregateByPage(fromTs, toTs);

        // 3) Y 轴(按总 PV 倒序)
        List<String> yAxis = new ArrayList<>();
        Map<String, Long> pageTotal = new LinkedHashMap<>();
        for (Map<String, Object> r : pageRows) {
            String name = (String) r.get("pageName");
            Long total = ((Number) r.get("totalVisits")).longValue();
            yAxis.add(name);
            pageTotal.put(name, total);
        }

        // 4) 矩阵填充
        int cols = xBuckets.size();
        long[][] matrix = new long[yAxis.size()][cols];
        Map<String, Integer> bucketIdx = new HashMap<>();
        for (int i = 0; i < cols; i++) bucketIdx.put(xBuckets.get(i), i);
        Map<String, Integer> pageIdx = new HashMap<>();
        for (int i = 0; i < yAxis.size(); i++) pageIdx.put(yAxis.get(i), i);

        long totalVisits = 0;
        long maxValue = 0L;
        for (Map<String, Object> r : raw) {
            String name = (String) r.get("pageName");
            Object bucketObj = r.get("bucket");
            String bucket = bucketObj == null ? "" : bucketObj.toString();
            Long pv = ((Number) r.get("pv")).longValue();
            Integer yi = pageIdx.get(name);
            Integer xi = bucketIdx.get(bucket);
            if (yi == null || xi == null) continue;
            matrix[yi][xi] = pv;
            totalVisits += pv;
            if (pv > maxValue) maxValue = pv;
        }

        // 5) KPI
        HeatmapResponse.Kpi kpi = new HeatmapResponse.Kpi();
        kpi.setTotalVisits(totalVisits);

        if (!yAxis.isEmpty()) {
            String topPage = yAxis.get(0);
            kpi.setTopPageName(topPage);
            kpi.setTopPageTotal(pageTotal.get(topPage));
        }

        int peakBucket = -1;
        long peakBucketTotal = 0L;
        for (int c = 0; c < cols; c++) {
            long sum = 0;
            for (int y = 0; y < yAxis.size(); y++) sum += matrix[y][c];
            if (sum > peakBucketTotal) {
                peakBucketTotal = sum;
                peakBucket = c;
            }
        }
        kpi.setPeakHour(peakBucket >= 0 ? peakBucket : 0);
        kpi.setPeakHourTotal(peakBucketTotal);

        // 6) 组装响应
        HeatmapResponse resp = new HeatmapResponse();
        resp.setWindow(window);
        resp.setFromTs(fromTs.toString());
        resp.setToTs(toTs.toString());
        resp.setDays(days);
        resp.setKpi(kpi);
        resp.setXAxis(xBuckets);
        resp.setXAxisUnit(unit);
        resp.setYAxis(yAxis);
        List<List<Long>> matrixOut = new ArrayList<>();
        for (long[] row : matrix) {
            List<Long> r = new ArrayList<>(cols);
            for (long v : row) r.add(v);
            matrixOut.add(r);
        }
        resp.setMatrix(matrixOut);
        resp.setMaxValue(maxValue);
        return resp;
    }

    /**
     * 不同 window 下的 X 轴分桶方案:
     *   1d  → 24 小时,unit=hour,      buckets=["00".."23"]
     *   7d  → 周一到周日,unit=dayOfWeek, buckets=DAYOFWEEK=[2,3,4,5,6,7,1]
     *   30d → 最近 30 天日期,unit=date, buckets=["05-18"..."06-16"]
     *   90d → 最近 13 周 ISO 周,unit=week, buckets=["2026-W20"..."2026-W25"]
     *   SQL 用同一个 aggregateByBucket(fromTs, toTs, mode) 走不同分桶。
     */
    private static XBucketPlan buildBuckets(String window, LocalDate today) {
        XBucketPlan p = new XBucketPlan();
        switch (window) {
            case "1d": {
                p.unit = "hour";
                p.mode = "hour";
                List<String> bs = new ArrayList<>(24);
                for (int h = 0; h < 24; h++) bs.add(String.format("%02d", h));
                p.buckets = bs;
                return p;
            }
            case "7d": {
                p.unit = "dayOfWeek";
                p.mode = "dayOfWeek";
                // 周一(2) 到 周日(1)
                p.buckets = List.of("2", "3", "4", "5", "6", "7", "1");
                return p;
            }
            case "30d": {
                p.unit = "date";
                p.mode = "date";
                List<String> bs = new ArrayList<>(30);
                for (int i = 30 - 1; i >= 0; i--) {
                    LocalDate d = today.minusDays(i);
                    bs.add(String.format("%02d-%02d", d.getMonthValue(), d.getDayOfMonth()));
                }
                p.buckets = bs;
                return p;
            }
            case "90d": {
                p.unit = "week";
                p.mode = "week";
                int weeks = (90 + 6) / 7; // 13
                List<String> bs = new ArrayList<>(weeks);
                for (int i = weeks - 1; i >= 0; i--) {
                    LocalDate d = today.minusDays((long) i * 7);
                    bs.add(isoWeekKey(d));
                }
                p.buckets = bs;
                return p;
            }
            default:
                throw new BusinessException(400, "window 非法");
        }
    }

    private static String isoWeekKey(LocalDate d) {
        WeekFields wf = WeekFields.ISO;
        int year = d.get(wf.weekBasedYear());
        int week = d.get(wf.weekOfWeekBasedYear());
        return String.format("%04d-W%02d", year, week);
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

    private static class XBucketPlan {
        String unit;       // hour | dayOfWeek | date | week
        String mode;       // SQL CASE 用的分桶键
        List<String> buckets;
    }
}
