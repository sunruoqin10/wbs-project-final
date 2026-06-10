package com.wbs.project.controller;

import com.wbs.project.common.Result;
import com.wbs.project.dto.OrgNode;
import com.wbs.project.service.OrgService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 组织架构接口
 */
@RestController
@RequestMapping("/orgs")
@RequiredArgsConstructor
public class OrgController {

    private final OrgService orgService;

    /**
     * 获取组织架构树（mdm_if_or_a 整表嵌套）
     * 任何登录用户可访问
     */
    @GetMapping("/tree")
    public Result<OrgNode> getTree() {
        return Result.success("获取组织架构成功", orgService.buildTree());
    }
}
