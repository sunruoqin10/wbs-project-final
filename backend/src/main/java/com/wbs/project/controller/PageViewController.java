package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.entity.PageView;
import com.wbs.project.exception.BusinessException;
import com.wbs.project.service.PageViewService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/page-views")
@RequiredArgsConstructor
public class PageViewController {

    private final PageViewService pageViewService;

    @PostMapping
    public Result<Void> record(
        @RequestBody PageView body,
        HttpServletRequest request
    ) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) throw new BusinessException(401, "未登录");

        // 校验
        if (body.getPagePath() == null || !body.getPagePath().startsWith("/")) {
            throw new BusinessException(400, "pagePath 必须以 / 开头");
        }
        if (body.getPageName() == null || body.getPageName().isBlank()) {
            throw new BusinessException(400, "pageName 不能为空");
        }
        if (body.getPagePath().length() > 255) {
            throw new BusinessException(400, "pagePath 长度超过 255");
        }
        if (body.getPageName().length() > 100) {
            throw new BusinessException(400, "pageName 长度超过 100");
        }
        // occurredAt 由 Jackson + jackson-datatype-jsr310 + PageView.@JsonFormat 直接反序列化为 LocalDateTime
        // (前端发 ISO 8601 带 T 即可,如 "2026-06-16T14:23:45")
        if (body.getOccurredAt() == null) {
            throw new BusinessException(400, "occurredAt 不能为空");
        }
        if (body.getOccurredAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            throw new BusinessException(400, "occurredAt 不能晚于当前时间 1 分钟");
        }

        pageViewService.record(userId, body.getPagePath(), body.getPageName(), body.getOccurredAt());
        return Result.success(null);
    }
}
