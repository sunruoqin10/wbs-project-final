INSERT IGNORE INTO sys_user (id, name, email, role, department, joined_at, created_at, updated_at, password)
SELECT
    p.EMP_NUM,
    p.EMP_NAM,
    p.EMAIL_ADDR,
    'member',
    o.ORG_NAM,
    NOW(),
    NOW(),
    NOW(),
    '1'
FROM mdm_if_pa_a p
LEFT JOIN mdm_if_or_a o ON p.ORG_CD = o.ORG_CD
WHERE p.EMP_NAM IS NOT NULL
  AND p.EMAIL_ADDR IS NOT NULL;