package me.north30.erp.system.common.util;

import lombok.RequiredArgsConstructor;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.vo.DeptTreeVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 组织树构建工具：一次查出组织后内存组树（禁止循环内查库）。
 * <p>与 {@link MenuTreeUtil} 同思路；差异点：按条件过滤后父节点可能不在结果集，
 * 此时将其子节点提升为根节点输出，避免数据静默丢失。</p>
 * <p>DeptTreeVO 为不可变 record，组树采用自底向上递归构造，同级按 deptSort 升序排列。</p>
 * <p>Entity → VO 转换委托 {@link DeptConverter}（MapStruct 唯一转换方案）。</p>
 */
@Component
@RequiredArgsConstructor
public class DeptTreeUtil {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private static final Comparator<DeptTreeVO> DEPT_SORT_COMPARATOR =
        Comparator.comparing(DeptTreeVO::deptSort, Comparator.nullsLast(Comparator.naturalOrder()));

    private final DeptConverter deptConverter;

    /**
     * 将平铺组织列表组装为树（parentId=0 或父级不在结果集中的节点为根），同级按 deptSort 升序排列。
     *
     * @param depts 平铺组织实体列表
     * @return 树形组织列表（无子节点时 children 为 null）
     */
    public List<DeptTreeVO> buildTree(List<SysDept> depts) {
        if (depts == null || depts.isEmpty()) {
            return new ArrayList<>();
        }
        List<DeptTreeVO> vos = depts.stream().map(deptConverter::toTreeVO).collect(Collectors.toList());
        Map<Long, List<DeptTreeVO>> childrenMap = vos.stream()
            .filter(vo -> vo.parentId() != null && vo.parentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(DeptTreeVO::parentId));
        // 根节点：parentId=0，或按条件过滤后父节点不在结果集中的节点
        return vos.stream()
            .filter(vo -> vo.parentId() == null || vo.parentId() == ROOT_PARENT_ID
                || childrenMap.containsKey(vo.id()) && !containsId(vos, vo.parentId()))
            .sorted(DEPT_SORT_COMPARATOR)
            .map(vo -> attachChildren(vo, vos, childrenMap))
            .collect(Collectors.toList());
    }

    /**
     * 自底向上递归装配子树（record 不可变，需逐层新建节点）。
     */
    private static DeptTreeVO attachChildren(DeptTreeVO vo, List<DeptTreeVO> vos,
                                             Map<Long, List<DeptTreeVO>> childrenMap) {
        List<DeptTreeVO> children = childrenMap.get(vo.id());
        if (children == null) {
            return vo;
        }
        List<DeptTreeVO> attached = children.stream()
            .sorted(DEPT_SORT_COMPARATOR)
            .map(child -> attachChildren(child, vos, childrenMap))
            .collect(Collectors.toList());
        return new DeptTreeVO(vo.id(), vo.deptCode(), vo.deptName(), vo.parentId(), vo.deptType(),
            vo.deptLevel(), vo.ancestors(), vo.leader(), vo.phone(), vo.deptSort(), vo.status(), attached);
    }

    /**
     * 判断结果集中是否存在指定 ID 的节点。
     */
    private static boolean containsId(List<DeptTreeVO> vos, Long id) {
        return vos.stream().anyMatch(vo -> id.equals(vo.id()));
    }

    /**
     * 实体 → 树 VO 转换（委托 DeptConverter）。
     */
    public DeptTreeVO toVO(SysDept dept) {
        return deptConverter.toTreeVO(dept);
    }
}
