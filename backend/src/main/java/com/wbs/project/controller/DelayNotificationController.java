package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.scheduler.DelayNotificationScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/delay-notifications")
@RequiredArgsConstructor
public class DelayNotificationController {

    private final DelayNotificationScheduler delayNotificationScheduler;

    @PostMapping("/trigger")
    public Result<String> triggerDelayCheck() {
        delayNotificationScheduler.checkAndSendDelayNotificationsManual();
        return Result.success("延期检查已触发");
    }
}
