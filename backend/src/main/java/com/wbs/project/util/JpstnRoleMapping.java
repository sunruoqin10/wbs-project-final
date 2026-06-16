package com.wbs.project.util;

import com.wbs.project.enums.UserRole;

/**
 * JPSTN_CD → role 推断常量(2026-06-16 引入)
 *
 * 触发条件(在 UserService 中判断):
 *   - 当前 sys_user.role == 'member'
 *   - 当前 sys_user.jpstnCd ∈ {BA, BF}
 *
 * 命中后由 UserService.syncHrData 步 ④ 升级 role / managed_* + 写 audit。
 *
 * 未来 BA/BF 之外的职级若需走默认映射,在此处加一行即可。
 */
public final class JpstnRoleMapping {

    /** BA → 部门项目负责人 */
    public static final String JPSTN_DEPT_PM   = "BA";
    /** BF → 项目经理 */
    public static final String JPSTN_PROJECT_PM = "BF";

    private JpstnRoleMapping() {}

    /**
     * @return 推断出的 UserRole.code;null 表示不升级(保持 member 或不动)
     */
    public static String inferRoleCode(String jpstnCd) {
        if (jpstnCd == null) return null;
        if (JPSTN_DEPT_PM.equals(jpstnCd))   return UserRole.DEPT_PROJECT_MANAGER.code;
        if (JPSTN_PROJECT_PM.equals(jpstnCd)) return UserRole.PROJECT_MANAGER.code;
        return null;
    }

    /** 给审计 reason 用的可读描述 */
    public static String describe(String jpstnCd) {
        String code = inferRoleCode(jpstnCd);
        if (code == null) return null;
        return "JPSTN_CD=" + jpstnCd + " → " + UserRole.fromCode(code).getDescription();
    }
}