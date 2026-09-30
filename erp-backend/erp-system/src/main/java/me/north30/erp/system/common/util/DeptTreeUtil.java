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
 * <p>Entity → VO 转换委托 {@link DeptConverter}（MapStruct 唯一转换方案）。</p>
 */
@Component
@RequiredArgsConstructor
public class DeptTreeUtil {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private final DeptConverter deptConverter;

    /**
     * 将平铺组织列表组装为树（parentId=0 或父级不在结果集的节点为根），同级按 deptSort 升序排列。
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
            .filter(vo -> vo.getParentId() != null && vo.getParentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(DeptTreeVO::getParentId));
        for (DeptTreeVO vo : vos) {
            List<DeptTreeVO> children = childrenMap.get(vo.getId());
            if (children != null) {
                children.sort(Comparator.comparing(DeptTreeVO::getDeptSort,
                    Comparator.nullsLast(Comparator.naturalOrder())));
                vo.setChildren(children);
            }
        }
        // 根节点：parentId=0，或按条件过滤后父节点不在结果集中的节点
        return vos.stream()
            .filter(vo -> vo.getParentId() == null || vo.getParentId() == ROOT_PARENT_ID
                || childrenMap.containsKey(vo.getId()) && !containsId(vos, vo.getParentId()))
            .sorted(Comparator.comparing(DeptTreeVO::getDeptSort,
                Comparator.nullsLast(Comparator.naturalOrder())))
            .collect(Collectors.toList());
    }

    /**
     * 判断结果集中是否存在指定 ID 的节点。
     */
    private static boolean containsId(List<DeptTreeVO> vos, Long id) {
        return vos.stream().anyMatch(vo -> id.equals(vo.getId()));
    }

    /**
     * 实体 → 树 VO 转换（委托 DeptConverter）。
     */
    public DeptTreeVO toVO(SysDept dept) {
        return deptConverter.toTreeVO(dept);
    }
}
