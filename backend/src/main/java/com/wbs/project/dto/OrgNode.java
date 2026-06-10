package com.wbs.project.dto;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 组织架构树节点 DTO
 *
 * 由 mdm_if_or_a 整张表组装成一棵嵌套树返回给前端。
 * children 在树组装时填充；DB 查出来时为空。
 */
@Data
public class OrgNode {
    /** 组织 code（mdm_if_or_a.ORG_CD） */
    private String code;
    /** 组织名（mdm_if_or_a.ORG_NAM） */
    private String name;
    /** 所属公司 code（mdm_if_or_a.COMPANY_CD，2700/8400） */
    private String companyCd;
    /** 父组织 code（mdm_if_or_a.PRNT_ORG_CD，根节点为 null） */
    private String parentCode;
    /** 组织层级数字（mdm_if_or_a.ORG_LVL_NUM） */
    private Integer level;
    /** 子组织列表（树组装时填充） */
    private List<OrgNode> children = new ArrayList<>();
}
