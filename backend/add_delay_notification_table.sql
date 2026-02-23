CREATE TABLE IF NOT EXISTS delay_notification_record (
    id VARCHAR(50) PRIMARY KEY,
    task_id VARCHAR(50) NOT NULL,
    project_id VARCHAR(50) NOT NULL,
    notified_user_id VARCHAR(50) NOT NULL,
    notified_email VARCHAR(255) NOT NULL,
    notification_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_date (task_id, notified_user_id, notification_date),
    INDEX idx_project_id (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='延期任务提醒记录表';
