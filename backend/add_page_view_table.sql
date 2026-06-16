-- ===============================================================
-- 2026-06-16: 新增 sys_page_view 表(供 admin 访问率热力图)
-- 来源 spec: docs/superpowers/specs/2026-06-16-page-access-heatmap-design.md
-- 范围: 记录每次前端路由切换的访问日志
-- ⚠️ 必须先执行本 SQL 再启动服务
-- ===============================================================
CREATE TABLE IF NOT EXISTS sys_page_view (
    id           BIGINT       NOT NULL AUTO_INCREMENT          COMMENT '主键',
    user_id      VARCHAR(20)  NOT NULL                        COMMENT '访问者 userId(C0000001 格式)',
    page_path    VARCHAR(255) NOT NULL                        COMMENT '前端路由路径,例 /projects/123',
    page_name    VARCHAR(100) NOT NULL                        COMMENT '路由 name 字段,例 ProjectDetail(便于 GROUP BY)',
    occurred_at  DATETIME     NOT NULL                        COMMENT '访问发生时间(精确到秒)',
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
    PRIMARY KEY (id),
    INDEX idx_pv_occurred_at (occurred_at),
    INDEX idx_pv_page_occurred (page_name, occurred_at),
    INDEX idx_pv_user_occurred (user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='页面访问日志';
