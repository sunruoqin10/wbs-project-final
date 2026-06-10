package com.wbs.project.mapper;

import com.wbs.project.dto.OrgNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 组织架构 Mapper
 * 查 mdm_if_or_a 中间表，返回扁平节点列表（树组装由 Service 层完成）
 */
@Mapper
public interface OrgMapper {

    /**
     * 查询所有组织节点（扁平）
     */
    List<OrgNode> selectAllOrgNodes();
}
