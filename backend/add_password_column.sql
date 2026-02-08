-- 添加 password 字段到 sys_user 表
ALTER TABLE sys_user ADD COLUMN password VARCHAR(255) COMMENT '明文密码';

-- 创建测试用户
INSERT INTO sys_user (id, name, email, avatar, role, department, skills, password, joined_at, created_at, updated_at)
VALUES (
    'u10000001',
    '测试用户',
    'test@example.com',
    'https://api.dicebear.com/7.x/avataaars/svg?seed=test',
    'admin',
    '技术部',
    '["Java", "Vue", "MySQL"]',
    '123456',
    NOW(),
    NOW(),
    NOW()
);
