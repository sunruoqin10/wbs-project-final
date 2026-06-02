package com.wbs.project.scheduler;

import com.wbs.project.entity.SchedulerConfig;
import com.wbs.project.mapper.SchedulerConfigMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerManager {

    private final SchedulerConfigMapper schedulerConfigMapper;
    private final ApplicationContext applicationContext;

    private ThreadPoolTaskScheduler taskScheduler;
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    private final Map<String, String> taskBeanMap = Map.of(
        "delay-notification", "delayNotificationScheduler"
    );

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(5);
        taskScheduler.setThreadNamePrefix("scheduler-");
        taskScheduler.initialize();

        List<SchedulerConfig> configs = schedulerConfigMapper.selectAll();
        for (SchedulerConfig config : configs) {
            if (Boolean.TRUE.equals(config.getEnabled())) {
                startScheduler(config);
            }
        }
        log.info("SchedulerManager initialized, loaded {} configs", configs.size());
    }

    public List<SchedulerConfig> getAllConfigs() {
        return schedulerConfigMapper.selectAll();
    }

    public SchedulerConfig getConfig(String id) {
        return schedulerConfigMapper.selectById(id);
    }

    public SchedulerConfig updateConfig(SchedulerConfig config) {
        SchedulerConfig existing = schedulerConfigMapper.selectById(config.getId());
        if (existing == null) {
            throw new RuntimeException("调度任务不存在: " + config.getId());
        }

        boolean wasRunning = Boolean.TRUE.equals(existing.getEnabled());

        if (wasRunning) {
            stopSchedulerInternal(config.getId());
        }

        schedulerConfigMapper.update(config);
        SchedulerConfig updated = schedulerConfigMapper.selectById(config.getId());

        if (Boolean.TRUE.equals(config.getEnabled())) {
            startScheduler(updated);
        }

        return updated;
    }

    public void startScheduler(String id) {
        SchedulerConfig config = schedulerConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("调度任务不存在: " + id);
        }
        config.setEnabled(true);
        schedulerConfigMapper.update(config);
        startScheduler(config);
    }

    public void stopScheduler(String id) {
        SchedulerConfig config = schedulerConfigMapper.selectById(id);
        if (config == null) {
            throw new RuntimeException("调度任务不存在: " + id);
        }
        config.setEnabled(false);
        schedulerConfigMapper.update(config);
        stopSchedulerInternal(id);
    }

    public void triggerNow(String id) {
        String beanName = taskBeanMap.get(id);
        if (beanName == null) {
            throw new RuntimeException("未知的调度任务: " + id);
        }
        Runnable task = (Runnable) applicationContext.getBean(beanName);
        try {
            task.run();
            schedulerConfigMapper.updateRunTime(id, LocalDateTime.now(), null);
            log.info("Task {} triggered manually", id);
        } catch (Exception e) {
            log.error("Error triggering task {}", id, e);
            throw new RuntimeException("任务执行失败: " + e.getMessage());
        }
    }

    private void startScheduler(SchedulerConfig config) {
        String beanName = taskBeanMap.get(config.getId());
        if (beanName == null) {
            log.warn("No bean mapping for scheduler: {}", config.getId());
            return;
        }

        try {
            Runnable task = (Runnable) applicationContext.getBean(beanName);
            ScheduledFuture<?> future = taskScheduler.schedule(task, new CronTrigger(config.getCronExpression()));
            scheduledTasks.put(config.getId(), future);
            log.info("Scheduler {} started with cron: {}", config.getId(), config.getCronExpression());
        } catch (Exception e) {
            log.error("Failed to start scheduler: {}", config.getId(), e);
        }
    }

    private void stopSchedulerInternal(String id) {
        ScheduledFuture<?> future = scheduledTasks.remove(id);
        if (future != null) {
            future.cancel(false);
            log.info("Scheduler {} stopped", id);
        }
    }
}
