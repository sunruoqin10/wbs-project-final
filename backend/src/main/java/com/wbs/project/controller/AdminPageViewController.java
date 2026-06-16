package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.HeatmapResponse;
import com.wbs.project.service.PageViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/page-views")
@RequiredArgsConstructor
public class AdminPageViewController {

    private final PageViewService pageViewService;

    @GetMapping("/heatmap")
    public Result<HeatmapResponse> heatmap(@RequestParam String window) {
        return Result.success(pageViewService.getHeatmap(window));
    }
}
