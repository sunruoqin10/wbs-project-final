package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.scheduler.SchedulerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delay-notifications")
@RequiredArgsConstructor
public class DelayNotificationController {

    private final SchedulerManager schedulerManager;

    @PostMapping("/trigger")
    public Result<String> triggerDelayCheck() {
        schedulerManager.triggerNow("delay-notification");
        return Result.success("延期检查已触发");
    }
}
