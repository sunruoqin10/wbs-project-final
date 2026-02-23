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
        try {
            delayNotificationScheduler.checkAndSendDelayNotificationsManual();
            return Result.success("延期检查已触发");
        } catch (Exception e) {
            return Result.error("触发延期检查失败: " + e.getMessage());
        }
    }
}
