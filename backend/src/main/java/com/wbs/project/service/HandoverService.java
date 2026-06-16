package com.wbs.project.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wbs.project.entity.Project;
import com.wbs.project.entity.ProjectHandoverLog;
import com.wbs.project.entity.User;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.dto.HandoverPreviewResponse;
import com.wbs.project.dto.ProjectHandoverLogDTO;
import com.wbs.project.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverService {

    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectHandoverLogMapper handoverLogMapper;
    private final PermissionService permissionService;
    private final UserService userService;
    private final OrgService orgService;
    private final EmailNotificationService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String HANDOVER_TYPE_PM = "PM_HANDOVER";
    private static final String HANDOVER_TYPE_DEPT_PM = "DEPT_PM_HANDOVER";
    private static final String HANDOVER_TYPE_DEPT_MERGE = "DEPT_MERGE";
    private static final String HANDOVER_TYPE_RESIGN_FREEZE = "RESIGN_FREEZE";

    /**
     * PM_HANDOVER(spec §4.1 / §5.1 上半)
     */
    @Transactional
    public HandoverResult handoverPm(String operatorId, String outgoingId, String successorId,
                                     List<String> projectIds, String reason) {
        // 1. 权限闸口
        permissionService.requireCanHandoverPM(operatorId, outgoingId);

        // 2. 出让人 / 继任者加载
        User outgoing = userMapper.selectById(outgoingId);
        User successor = userMapper.selectById(successorId);
        if (successor == null) throw new BusinessException(404, "继任者不存在: " + successorId);
        if (!"project-manager".equals(outgoing.getRole())) {
            throw new BusinessException(400, "出让人角色必须为 project-manager");
        }
        if (!"project-manager".equals(successor.getRole())) {
            throw new BusinessException(400, "继任者角色必须为 project-manager");
        }
        if (outgoingId.equals(successorId)) {
            throw new BusinessException(400, "出让人与继任者不能是同一人");
        }
        if (outgoing.getCompanyCd() == null || !outgoing.getCompanyCd().equals(successor.getCompanyCd())) {
            throw new BusinessException(400, "出让人与继任者必须同公司 (outgoing.companyCd="
                    + outgoing.getCompanyCd() + ", successor.companyCd=" + successor.getCompanyCd() + ")");
        }

        // 3. 项目集合
        List<String> finalIds;
        if (projectIds == null || projectIds.isEmpty()) {
            finalIds = projectMapper.selectHandoverableByOwnerId(outgoingId)
                    .stream().map(Project::getId).collect(Collectors.toList());
        } else {
            List<String> candidate = projectMapper.selectHandoverableByOwnerId(outgoingId)
                    .stream().map(Project::getId).collect(Collectors.toList());
            if (!candidate.containsAll(projectIds)) {
                throw new BusinessException(400, "部分 projectIds 不属于出让人或不可交接");
            }
            finalIds = projectIds;
        }
        if (finalIds.isEmpty()) {
            throw new BusinessException(400, "出让人名下无可交接项目");
        }

        // 4. 行级锁
        userMapper.lockUsersForUpdate(Arrays.asList(outgoingId, successorId));   // 见 Step 3 提示
        projectMapper.lockByIds(finalIds);

        // 5. 写
        for (String pid : finalIds) {
            // 5a. project owner + previous_owner
            Project upd = new Project();
            upd.setId(pid);
            upd.setOwnerId(successorId);
            upd.setPreviousOwnerId(outgoingId);
            upd.setUpdatedAt(LocalDateTime.now());
            projectMapper.update(upd);   // 复用 update,只覆盖 set 过的字段
            // 5b. project_member owner 迁移
            int affected = projectMemberMapper.migrateOwner(pid, outgoingId, successorId);
            if (affected == 0) {
                // Plan originally referenced com.wbs.project.entity.ProjectMember (no such class);
                // fall back to existing 2-arg insert(projectId, userId). Role defaults via DB schema.
                projectMemberMapper.insert(pid, successorId);
            }
            // 5c. 审计
            ProjectHandoverLog log = new ProjectHandoverLog();
            log.setProjectId(pid);
            log.setHandoverType(HANDOVER_TYPE_PM);
            log.setFromUserId(outgoingId);
            log.setToUserId(successorId);
            log.setReason(reason);
            log.setOperatorId(operatorId);
            log.setCreatedAt(LocalDateTime.now());
            handoverLogMapper.insert(log);
        }

        // 6. 调整 managed_project_ids(应用层读 → 过滤 → 写回)
        try {
            List<String> outgoingIds = userService.parseManagedDeptCodes(
                    userMapper.selectById(outgoingId).getManagedProjectIds());
            outgoingIds.removeAll(finalIds);
            String outgoingJson = objectMapper.writeValueAsString(outgoingIds);
            userMapper.updateManagedProjectIds(outgoingId, outgoingJson);

            List<String> successorIds = userService.parseManagedDeptCodes(
                    userMapper.selectById(successorId).getManagedProjectIds());
            for (String id : finalIds) {
                if (!successorIds.contains(id)) successorIds.add(id);
            }
            String successorJson = objectMapper.writeValueAsString(successorIds);
            userMapper.updateManagedProjectIds(successorId, successorJson);
        } catch (Exception e) {
            throw new BusinessException(500, "managed_project_ids 操作失败: " + e.getMessage());
        }

        // 7. token_version + 1
        userMapper.bumpTokenVersion(outgoingId);
        userMapper.bumpTokenVersion(successorId);

        // 8. 异步通知
        emailService.sendHandoverNotifications(operatorId, outgoingId, successorId, finalIds);

        return new HandoverResult(HANDOVER_TYPE_PM, finalIds.size(), finalIds, null);
    }

    /** DEPT_PM_HANDOVER(spec §4.1 / §5.1 下半) */
    @Transactional
    public HandoverResult handoverDeptPm(String operatorId, String outgoingId, String successorId,
                                         List<String> deptCodes, String reason) {
        if (deptCodes == null || deptCodes.isEmpty()) {
            throw new BusinessException(400, "deptCodes 不能为空");
        }
        permissionService.requireCanHandoverDeptPM(operatorId, outgoingId, deptCodes);

        User outgoing = userMapper.selectById(outgoingId);
        User successor = userMapper.selectById(successorId);
        if (successor == null) throw new BusinessException(404, "继任者不存在: " + successorId);
        if (!"dept-project-manager".equals(outgoing.getRole())) {
            throw new BusinessException(400, "出让人角色必须为 dept-project-manager");
        }
        if (!"dept-project-manager".equals(successor.getRole())) {
            throw new BusinessException(400, "继任者角色必须为 dept-project-manager");
        }
        if (outgoingId.equals(successorId)) {
            throw new BusinessException(400, "出让人与继任者不能是同一人");
        }
        if (outgoing.getCompanyCd() == null || !outgoing.getCompanyCd().equals(successor.getCompanyCd())) {
            throw new BusinessException(400, "出让人与继任者必须同公司");
        }

        // 校验 deptCodes ⊆ outgoing.managedDeptCodes
        List<String> outgoingCodes = userService.parseManagedDeptCodes(outgoing.getManagedDeptCodes());
        if (!outgoingCodes.containsAll(deptCodes)) {
            throw new BusinessException(400, "deptCodes 包含出让人未管辖的部门");
        }

        // 行级锁
        userMapper.lockUsersForUpdate(Arrays.asList(outgoingId, successorId));

        // 调整 managed_dept_codes
        try {
            outgoingCodes.removeAll(deptCodes);
            String outgoingJson = objectMapper.writeValueAsString(outgoingCodes);
            userMapper.updateManagedDeptCodes(outgoingId, outgoingJson);

            List<String> successorCodes = userService.parseManagedDeptCodes(successor.getManagedDeptCodes());
            for (String code : deptCodes) {
                if (!successorCodes.contains(code)) successorCodes.add(code);
            }
            String successorJson = objectMapper.writeValueAsString(successorCodes);
            userMapper.updateManagedDeptCodes(successorId, successorJson);
        } catch (Exception e) {
            throw new BusinessException(500, "managed_dept_codes 操作失败: " + e.getMessage());
        }

        // token_version + 1
        userMapper.bumpTokenVersion(outgoingId);
        userMapper.bumpTokenVersion(successorId);

        // 通知:受影响 PM(部门下项目 owner)
        List<String> affectedPmIds = projectMapper.selectPmIdsByDeptCodes(deptCodes);
        emailService.sendDeptHandoverNotifications(operatorId, outgoingId, successorId, deptCodes, affectedPmIds);

        // Dept-PM 场景不写 sys_project_handover_log(语义是管辖范围变更非项目 owner 变更,见 spec §5.1)
        return new HandoverResult(HANDOVER_TYPE_DEPT_PM, deptCodes.size(), null, deptCodes);
    }

    /** DEPT_MERGE(spec §4.2):把旧部门的在途项目搬到新部门,并把管辖权交给继任 dept-PM */
    @Transactional
    public DeptMergeResult mergeDept(String operatorId, String oldCode, String newCode,
                                     String successorId, String reason) {
        // 1. 权限闸口:仅 admin
        if (!permissionService.isAdmin(operatorId)) {
            throw new BusinessException(403, "仅 admin 可执行 DEPT_MERGE");
        }
        // 2. 参数校验
        if (oldCode == null || oldCode.isEmpty() || newCode == null || newCode.isEmpty()) {
            throw new BusinessException(400, "oldCode / newCode 不能为空");
        }
        if (oldCode.equals(newCode)) {
            throw new BusinessException(400, "oldCode 与 newCode 不能相同");
        }

        // 3. 旧部门必须有在途项目
        int activeCount = projectMapper.countActiveByDeptCode(oldCode);
        if (activeCount <= 0) {
            throw new BusinessException(400, "旧部门 " + oldCode + " 无在途项目,无需合并");
        }
        // 4. 新部门必须有人
        if (userMapper.countByDeptCode(newCode) <= 0) {
            throw new BusinessException(400, "新部门 " + newCode + " 下无在职用户");
        }

        // 5. 继任者校验
        User successor = userMapper.selectById(successorId);
        if (successor == null) throw new BusinessException(404, "继任者不存在: " + successorId);
        if (!"dept-project-manager".equals(successor.getRole())) {
            throw new BusinessException(400, "继任者角色必须为 dept-project-manager");
        }
        // 6. 继任者与新部门必须同公司
        String newDeptCompany = orgService.getCompanyByDeptCode(newCode);
        if (newDeptCompany == null || !newDeptCompany.equals(successor.getCompanyCd())) {
            throw new BusinessException(400, "继任者与新部门 " + newCode + " 必须同公司 "
                    + "(newDeptCompany=" + newDeptCompany
                    + ", successor.companyCd=" + successor.getCompanyCd() + ")");
        }

        // 7. 找旧部门 PM(可能为空)
        List<String> oldDeptPmIds = userMapper.selectIdsByManagedDeptCode(oldCode);
        String oldDeptPmId = (oldDeptPmIds == null || oldDeptPmIds.isEmpty())
                ? null : oldDeptPmIds.get(0);

        // 8. 行级锁(继任者必锁;旧部门 PM 若存在也锁)
        List<String> lockIds = new ArrayList<>();
        lockIds.add(successorId);
        if (oldDeptPmId != null && !oldDeptPmId.equals(successorId)) {
            lockIds.add(oldDeptPmId);
        }
        userMapper.lockUsersForUpdate(lockIds);

        // 9. 迁移项目 dept_code
        int affected = projectMapper.updateDeptCodeForActiveProjects(oldCode, newCode);
        if (affected <= 0) {
            throw new BusinessException(500, "迁移项目 dept_code 失败(affected=" + affected + ")");
        }

        // 10. 取审计 ID 列表(排除 completed/cancelled)
        List<String> affectedIds = projectMapper.selectIdsByDeptCodesAndStatus(
                Arrays.asList(newCode),
                Arrays.asList("completed", "cancelled"),
                true);

        // 11. 调整 managed_dept_codes
        try {
            if (oldDeptPmId != null) {
                User oldDeptPm = userMapper.selectById(oldDeptPmId);
                if (oldDeptPm != null) {
                    List<String> oldCodes = userService.parseManagedDeptCodes(
                            oldDeptPm.getManagedDeptCodes());
                    oldCodes.remove(oldCode);
                    String json = objectMapper.writeValueAsString(oldCodes);
                    userMapper.updateManagedDeptCodes(oldDeptPmId, json);
                }
            }
            // 继任者:加 newCode(去重)
            List<String> successorCodes = userService.parseManagedDeptCodes(
                    successor.getManagedDeptCodes());
            if (!successorCodes.contains(newCode)) {
                successorCodes.add(newCode);
            }
            String successorJson = objectMapper.writeValueAsString(successorCodes);
            userMapper.updateManagedDeptCodes(successorId, successorJson);
        } catch (Exception e) {
            throw new BusinessException(500, "managed_dept_codes 操作失败: " + e.getMessage());
        }

        // 12. 审计日志:每个受影响项目写一条
        LocalDateTime now = LocalDateTime.now();
        for (String pid : affectedIds) {
            ProjectHandoverLog log = new ProjectHandoverLog();
            log.setProjectId(pid);
            log.setHandoverType(HANDOVER_TYPE_DEPT_MERGE);
            log.setFromDeptCode(oldCode);
            log.setToDeptCode(newCode);
            log.setFromUserId(oldDeptPmId);
            log.setToUserId(successorId);
            log.setReason(reason);
            log.setOperatorId(operatorId);
            log.setCreatedAt(now);
            handoverLogMapper.insert(log);
        }

        // 13. token_version + 1
        if (oldDeptPmId != null && !oldDeptPmId.equals(successorId)) {
            userMapper.bumpTokenVersion(oldDeptPmId);
        }
        userMapper.bumpTokenVersion(successorId);

        // 14. 通知:新部门下项目 owner(在途)
        List<String> affectedPmIds = projectMapper.selectPmIdsByDeptCodes(Arrays.asList(newCode));
        emailService.sendDeptHandoverNotifications(operatorId, oldDeptPmId, successorId,
                Arrays.asList(oldCode, newCode), affectedPmIds);

        return new DeptMergeResult(affected, newCode);
    }

    /**
     * HR 同步副作用:对每个离职用户,按角色冻结项目归属(spec §2.3 / §4.3)
     * 在 UserService.syncHrData 已有 @Transactional 内调用
     */
    @Transactional
    public void handleResigned(List<String> resignedUserIds) {
        if (resignedUserIds == null || resignedUserIds.isEmpty()) return;

        for (String userId : resignedUserIds) {
            User user = userMapper.selectById(userId);
            if (user == null) continue;

            boolean tokenAlreadyBumped = false;
            switch (user.getRole() == null ? "" : user.getRole()) {
                case "project-manager": {
                    List<String> ownedActiveIds = projectMapper.selectIdsByActiveOwner(userId);
                    for (String pid : ownedActiveIds) {
                        Project upd = new Project();
                        upd.setId(pid);
                        upd.setOwnerId(null);
                        upd.setPreviousOwnerId(userId);
                        upd.setNeedsHandover(true);
                        upd.setUpdatedAt(LocalDateTime.now());
                        projectMapper.update(upd);
                    }
                    userMapper.updateManagedProjectIds(userId, "[]");
                    for (String pid : ownedActiveIds) {
                        ProjectHandoverLog l = new ProjectHandoverLog();
                        l.setProjectId(pid);
                        l.setHandoverType(HANDOVER_TYPE_RESIGN_FREEZE);
                        l.setFromUserId(userId);
                        l.setReason("离职冻结");
                        l.setOperatorId("SYSTEM_HR_SYNC");
                        l.setCreatedAt(LocalDateTime.now());
                        handoverLogMapper.insert(l);
                    }
                    break;
                }
                case "dept-project-manager": {
                    List<String> codes = userService.parseManagedDeptCodes(user.getManagedDeptCodes());
                    if (!codes.isEmpty()) {
                        projectMapper.markNeedsHandoverByDeptCodes(codes);
                    }
                    // 清空 managed_dept_codes / managed_company_cd。
                    // 注:UserMapper.update 是 selective 的,不写 managed_company_cd;
                    // 用 updateRoleAndScope 才能正确清掉(顺带 token_version+1)。
                    userMapper.updateRoleAndScope(userId, user.getRole(), "[]", null, null);
                    tokenAlreadyBumped = true;
                    break;
                }
                default: {
                    // member / viewer / admin:不动
                    break;
                }
            }

            if (!tokenAlreadyBumped) {
                userMapper.bumpTokenVersion(userId);
            }
            emailService.sendResignFreezeNotifications(userId, projectMapper.selectIdsByActiveOwner(userId));
        }
    }

    public HandoverPreviewResponse preview(String operatorId, String userId, String type) {
        User outgoing = userMapper.selectById(userId);
        if (outgoing == null) throw new BusinessException(404, "用户不存在: " + userId);

        if ("PM_HANDOVER".equals(type)) {
            permissionService.requireCanHandoverPM(operatorId, userId);
        } else if ("DEPT_PM_HANDOVER".equals(type)) {
            List<String> codes = userService.parseManagedDeptCodes(outgoing.getManagedDeptCodes());
            permissionService.requireCanHandoverDeptPM(operatorId, userId, codes);
        } else {
            throw new BusinessException(400, "不支持的 type: " + type);
        }

        HandoverPreviewResponse resp = new HandoverPreviewResponse();
        HandoverPreviewResponse.OutgoingBrief brief = new HandoverPreviewResponse.OutgoingBrief();
        brief.setUserId(outgoing.getId());
        brief.setName(outgoing.getName());
        brief.setRole(outgoing.getRole());
        brief.setDeptCode(outgoing.getDeptCode());
        resp.setOutgoing(brief);

        if ("PM_HANDOVER".equals(type)) {
            List<com.wbs.project.dto.ProjectBriefRow> rows =
                    projectMapper.selectProjectBriefRowsByOwnerId(userId);
            List<HandoverPreviewResponse.ProjectBrief> briefs = rows.stream().map(r -> {
                HandoverPreviewResponse.ProjectBrief b = new HandoverPreviewResponse.ProjectBrief();
                b.setId(r.getId());
                b.setName(r.getName());
                b.setStatus(r.getStatus());
                b.setDeptCode(r.getDeptCode());
                b.setPreviousOwnerId(r.getPreviousOwnerId());
                b.setNeedsHandover(r.getNeedsHandover());
                return b;
            }).collect(Collectors.toList());
            resp.setCandidateProjects(briefs);
        } else {
            resp.setCandidateDeptCodes(userService.parseManagedDeptCodes(outgoing.getManagedDeptCodes()));
        }
        return resp;
    }

    public Map<String, Object> history(String requesterId, String userId, String type, int page, int pageSize) {
        if (!permissionService.isAdmin(requesterId) && !requesterId.equals(userId)) {
            throw new BusinessException(403, "仅 admin 或本人可查看交接历史");
        }

        int offset = (page - 1) * pageSize;
        List<ProjectHandoverLog> records = handoverLogMapper.selectByUserId(userId, type, offset, pageSize);
        int total = handoverLogMapper.countByUserId(userId, type);

        List<ProjectHandoverLogDTO> dtos = records.stream().map(r -> {
            ProjectHandoverLogDTO d = new ProjectHandoverLogDTO();
            d.setId(r.getId());
            d.setProjectId(r.getProjectId());
            d.setHandoverType(r.getHandoverType());
            d.setFromUserId(r.getFromUserId());
            d.setToUserId(r.getToUserId());
            d.setFromDeptCode(r.getFromDeptCode());
            d.setToDeptCode(r.getToDeptCode());
            d.setReason(r.getReason());
            d.setOperatorId(r.getOperatorId());
            d.setCreatedAt(r.getCreatedAt());
            if (r.getFromUserId() != null) {
                User u = userMapper.selectById(r.getFromUserId());
                if (u != null) d.setFromUserName(u.getName() != null ? u.getName() : "已离职");
            }
            if (r.getToUserId() != null) {
                User u = userMapper.selectById(r.getToUserId());
                if (u != null) d.setToUserName(u.getName() != null ? u.getName() : "已离职");
            }
            if (r.getProjectId() != null) {
                Project p = projectMapper.selectById(r.getProjectId());
                if (p != null) d.setProjectName(p.getName());
            }
            if (r.getOperatorId() != null) {
                User u = userMapper.selectById(r.getOperatorId());
                if (u != null) d.setOperatorName(u.getName() != null ? u.getName() : "已离职");
            }
            return d;
        }).collect(Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("total", total);
        result.put("records", dtos);
        return result;
    }

    /** 内部 DTO:统一返回给 Controller */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class HandoverResult {
        private String type;
        private int count;
        private List<String> projectIds;   // PM_HANDOVER 填充
        private List<String> deptCodes;    // DEPT_PM_HANDOVER 填充
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class DeptMergeResult {
        private int affectedProjectCount;
        private String newDeptCode;
    }
}