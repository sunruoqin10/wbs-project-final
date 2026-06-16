package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.*;
import com.wbs.project.service.HandoverService;
import com.wbs.project.service.HandoverService.HandoverResult;
import com.wbs.project.service.HandoverService.DeptMergeResult;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class HandoverController {

    private final HandoverService handoverService;

    @PutMapping("/users/{id}/handover")
    public Result<?> handover(@PathVariable String id,
                              @RequestBody Map<String, Object> body,
                              HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        String type = (String) body.getOrDefault("handoverType", "PM_HANDOVER");
        String successorId = (String) body.get("successorUserId");
        String reason = (String) body.get("reason");

        if ("PM_HANDOVER".equals(type)) {
            @SuppressWarnings("unchecked")
            List<String> projectIds = (List<String>) body.get("projectIds");
            HandoverResult r = handoverService.handoverPm(operatorId, id, successorId, projectIds, reason);
            return Result.success("交接成功",
                new PmHandoverResponse(r.getCount(), r.getProjectIds()));
        } else if ("DEPT_PM_HANDOVER".equals(type)) {
            @SuppressWarnings("unchecked")
            List<String> deptCodes = (List<String>) body.get("deptCodes");
            HandoverResult r = handoverService.handoverDeptPm(operatorId, id, successorId, deptCodes, reason);
            return Result.success("部门管辖权交接成功",
                new DeptPmHandoverResponse(r.getCount(), r.getDeptCodes()));
        }
        return Result.error(400, "不支持的 handoverType: " + type);
    }

    @PutMapping("/orgs/dept-merge")
    public Result<DeptMergeResponse> mergeDept(@RequestBody DeptMergeRequest req,
                                                HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        DeptMergeResult r = handoverService.mergeDept(operatorId,
                req.getOldDeptCode(), req.getNewDeptCode(),
                req.getSuccessorDeptManagerId(), req.getReason());
        return Result.success("部门合并完成",
                new DeptMergeResponse(r.getAffectedProjectCount(), r.getNewDeptCode()));
    }

    @GetMapping("/users/{id}/handover-preview")
    public Result<HandoverPreviewResponse> handoverPreview(@PathVariable String id,
                                                            @RequestParam(required = false, defaultValue = "PM_HANDOVER") String type,
                                                            HttpServletRequest request) {
        String operatorId = (String) request.getAttribute("userId");
        HandoverPreviewResponse data = handoverService.preview(operatorId, id, type);
        return Result.success(data);
    }

    @GetMapping("/users/{id}/project-handover-history")
    public Result<Map<String, Object>> projectHandoverHistory(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "20") int pageSize,
            @RequestParam(required = false) String handoverType,
            HttpServletRequest request) {
        String requesterId = (String) request.getAttribute("userId");
        if (pageSize > 100) pageSize = 100;
        return Result.success(handoverService.history(requesterId, id, handoverType, page, pageSize));
    }
}
