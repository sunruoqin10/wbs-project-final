package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.SchedulerConfig;
import com.wbs.project.scheduler.SchedulerManager;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/scheduler")
@RequiredArgsConstructor
public class SchedulerConfigController {

    private final SchedulerManager schedulerManager;

    private void checkAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("userRole");
        if (role == null || !"admin".equals(role)) {
            throw new RuntimeException("仅管理员可操作");
        }
    }

    @GetMapping("/configs")
    public Result<List<SchedulerConfig>> getAllConfigs(HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(schedulerManager.getAllConfigs());
    }

    @GetMapping("/configs/{id}")
    public Result<SchedulerConfig> getConfig(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        return Result.success(schedulerManager.getConfig(id));
    }

    @PutMapping("/configs/{id}")
    public Result<SchedulerConfig> updateConfig(@PathVariable String id,
                                                 @RequestBody SchedulerConfig config,
                                                 HttpServletRequest request) {
        checkAdmin(request);
        config.setId(id);
        return Result.success(schedulerManager.updateConfig(config));
    }

    @PostMapping("/{id}/start")
    public Result<String> startScheduler(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.startScheduler(id);
        return Result.success("任务已启动");
    }

    @PostMapping("/{id}/stop")
    public Result<String> stopScheduler(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.stopScheduler(id);
        return Result.success("任务已停止");
    }

    @PostMapping("/{id}/trigger")
    public Result<String> triggerNow(@PathVariable String id, HttpServletRequest request) {
        checkAdmin(request);
        schedulerManager.triggerNow(id);
        return Result.success("任务已手动触发");
    }
}
