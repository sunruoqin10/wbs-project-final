CREATE TABLE IF NOT EXISTS email_log (
    id VARCHAR(50) PRIMARY KEY,
    to_email VARCHAR(255) NOT NULL COMMENT '收件人邮箱',
    cc_email VARCHAR(255) COMMENT '抄送人邮箱',
    subject VARCHAR(500) NOT NULL COMMENT '邮件主题',
    template_name VARCHAR(100) COMMENT '邮件模板名称',
    status VARCHAR(20) NOT NULL COMMENT '发送状态：success/failed',
    error_message TEXT COMMENT '错误信息',
    sent_at DATETIME COMMENT '发送时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_to_email (to_email),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邮件发送记录表';
