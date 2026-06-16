package com.wbs.project.service;

import com.wbs.project.dto.OrgNode;
import com.wbs.project.mapper.OrgMapper;
import com.wbs.project.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 组织架构服务
 * 把 mdm_if_or_a 扁平数据组装成「按公司分组」的嵌套树返回给前端
 *
 * 顶层结构：4 个公司节点（2700 / 8400 / 2710 / 9000 ...）
 * 每个公司节点下挂该公司的所有 org 节点（保留父子关系）
 * 未匹配到任何公司码的 org 节点（极少见）会被收纳到 "__unassigned__" 节点
 */
@Service
@RequiredArgsConstructor
public class OrgService {

    private final OrgMapper orgMapper;
    private final UserMapper userMapper;

    /**
     * 通过 dept_code 反查所属 company_cd(spec §4.2 step 1)
     * 数据源:sys_user(本项目无 sys_org 表,任何 dept_code 都至少有 1 个用户)
     * @return company_cd 或 null(dept_code 不存在)
     */
    public String getCompanyByDeptCode(String deptCode) {
        if (deptCode == null || deptCode.isEmpty()) return null;
        return userMapper.selectCompanyCdByDeptCode(deptCode);
    }

    /**
     * 公司名映射（已知公司）
     */
    private static final Map<String, String> KNOWN_COMPANY_NAMES = Map.of(
            "2700", "运营法人",
            "8400", "建设法人",
            "2710", "运营法人(分社)",
            "9000", "其他"
    );

    public OrgNode buildTree() {
        List<OrgNode> flat = orgMapper.selectAllOrgNodes();

        // 索引所有 org 节点
        Map<String, OrgNode> byCode = new HashMap<>(flat.size() * 2);
        for (OrgNode n : flat) {
            byCode.put(n.getCode(), n);
        }

        // 1) 先按原 parentCode 关系挂载到原树
        //    找到真正的根（SKHYCL 这类）作为 virtualRoot
        OrgNode virtualRoot = new OrgNode();
        virtualRoot.setCode(null);
        virtualRoot.setName("virtual");
        virtualRoot.setLevel(0);

        for (OrgNode n : flat) {
            String parentCode = n.getParentCode();
            OrgNode parent = (parentCode != null && !parentCode.isEmpty())
                    ? byCode.get(parentCode)
                    : null;
            if (parent != null) {
                parent.getChildren().add(n);
            } else {
                virtualRoot.getChildren().add(n);
            }
        }

        // 2) 收集所有出现过的 company_cd
        Set<String> companyCodes = new TreeSet<>();
        for (OrgNode n : flat) {
            String cd = n.getCompanyCd();
            if (cd != null && !cd.isEmpty()) {
                companyCodes.add(cd);
            }
        }

        // 3) 收集每个 company_cd 涉及的 org 节点集合（含祖先链）
        //    这样保证父节点即便本身 company_cd 为空，只要某个后代是某公司，也会被纳入该公司
        Map<String, Set<String>> codesByCompany = new HashMap<>();
        Map<String, String> codeToCompany = new HashMap<>();
        for (OrgNode n : flat) {
            String cd = n.getCompanyCd();
            if (cd != null && !cd.isEmpty()) {
                codesByCompany.computeIfAbsent(cd, k -> new HashSet<>()).add(n.getCode());
                codeToCompany.put(n.getCode(), cd);
            }
        }
        // 把祖先链也加到对应公司集合
        for (Map.Entry<String, Set<String>> e : codesByCompany.entrySet()) {
            Set<String> codes = new HashSet<>(e.getValue());
            for (String c : e.getValue()) {
                String cur = c;
                while (cur != null) {
                    OrgNode node = byCode.get(cur);
                    if (node == null) break;
                    String parentCd = node.getParentCode();
                    if (parentCd == null || parentCd.isEmpty()) break;
                    codes.add(parentCd);
                    cur = parentCd;
                }
            }
            e.setValue(codes);
        }

        // 4) 构建顶层公司节点
        OrgNode resultRoot = new OrgNode();
        resultRoot.setCode("companies");
        resultRoot.setName("Companies");
        resultRoot.setLevel(0);

        // 公司名排序：已知公司优先，未知公司排后面
        List<String> sortedCompanies = new ArrayList<>(companyCodes);
        sortedCompanies.sort((a, b) -> {
            int ai = companyOrder(a);
            int bi = companyOrder(b);
            if (ai != bi) return Integer.compare(ai, bi);
            return a.compareTo(b);
        });

        for (String companyCd : sortedCompanies) {
            OrgNode companyNode = new OrgNode();
            companyNode.setCode(companyCd);
            companyNode.setName(KNOWN_COMPANY_NAMES.getOrDefault(companyCd, companyCd));
            companyNode.setCompanyCd(companyCd);
            companyNode.setLevel(1);
            resultRoot.getChildren().add(companyNode);

            // 把该公司涉及的 org 节点（深拷贝 + 过滤 children）挂到公司节点下
            Set<String> codes = codesByCompany.get(companyCd);
            // 找出该公司的"最顶层 org"（在该集合内且父节点不在该集合内，或者父节点不在 byCode）
            // 同时：复制节点结构（避免污染原始节点）
            Map<String, OrgNode> clonedByCode = new HashMap<>();
            for (String code : codes) {
                OrgNode src = byCode.get(code);
                if (src == null) continue;
                OrgNode copy = cloneShallow(src);
                clonedByCode.put(code, copy);
            }
            // 挂父子关系
            for (String code : codes) {
                OrgNode src = byCode.get(code);
                OrgNode copy = clonedByCode.get(code);
                if (src == null || copy == null) continue;
                String parentCd = src.getParentCode();
                OrgNode parentCopy = (parentCd != null) ? clonedByCode.get(parentCd) : null;
                if (parentCopy != null) {
                    parentCopy.getChildren().add(copy);
                } else {
                    companyNode.getChildren().add(copy);
                }
            }
        }

        return resultRoot;
    }

    private int companyOrder(String cd) {
        return switch (cd) {
            case "2700" -> 0;
            case "8400" -> 1;
            case "2710" -> 2;
            default -> 100;
        };
    }

    /**
     * 浅拷贝 org 节点（仅自身字段 + 空 children）
     */
    private OrgNode cloneShallow(OrgNode src) {
        OrgNode n = new OrgNode();
        n.setCode(src.getCode());
        n.setName(src.getName());
        n.setCompanyCd(src.getCompanyCd());
        n.setParentCode(src.getParentCode());
        n.setLevel(src.getLevel());
        return n;
    }
}
