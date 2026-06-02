-- init_scheduler_config.sql
CREATE TABLE IF NOT EXISTS sys_scheduler_config (
    id VARCHAR(32) NOT NULL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    cron_expression VARCHAR(50) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    last_run_time DATETIME,
    next_run_time DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_scheduler_config (id, name, description, cron_expression, enabled)
VALUES ('delay-notification', '延期通知', '每天检查延期任务并发送邮件通知', '0 0 9 * * ?', 1)
ON DUPLICATE KEY UPDATE name=VALUES(name);
