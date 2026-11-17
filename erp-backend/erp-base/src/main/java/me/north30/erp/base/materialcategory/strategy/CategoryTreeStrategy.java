package me.north30.erp.base.materialcategory.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

/**
 * 物料分类树结构策略：树构造与同级排序重排（可命名复用逻辑）。
 * <p>树构造一次查全量后内存组装（禁 N+1），自底向上递归构造、同级按 categorySort 升序；
 * 与 sys dept/menu 树构造同思路：按条件过滤后父节点不在结果集时，将其子节点提升为根节点输出。</p>
 * <p>erp-base 不依赖 erp-system，同级规范化重排逻辑在本策略内实现（语义与 system
 * {@code SiblingSortUtil} 一致：先按 10,20,30… 规范化重排，再交换目标与相邻节点位置）。</p>
 */
@Component
public class CategoryTreeStrategy {

    /** 顶级分类父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 规范化重排的排序步长 */
    private static final int SORT_STEP = 10;

    private static final Comparator<CategoryTreeVO> CATEGORY_SORT_COMPARATOR =
        Comparator.comparing(CategoryTreeVO::categorySort, Comparator.nullsLast(Comparator.naturalOrder()));

    /**
     * 将平铺分类 VO 组装为两级树（parentId=0 或父级不在结果集中的节点为根），
     * 同级按 categorySort 升序排列。实体 → VO 转换由 CategoryConverter 承担，本策略只负责结构装配。
     *
     * @param categories 平铺分类 VO 列表
     * @return 树形分类列表（无子节点时 children 为 null）
     */
    public List<CategoryTreeVO> buildTree(List<CategoryTreeVO> categories) {
        if (categories == null || categories.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> existingIds = categories.stream().map(CategoryTreeVO::id).collect(Collectors.toSet());
        Map<Long, List<CategoryTreeVO>> childrenMap = categories.stream()
            .filter(vo -> vo.parentId() != null && vo.parentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(CategoryTreeVO::parentId));
        // 根节点：parentId=0，或按条件过滤后父节点不在结果集中的节点
        return categories.stream()
            .filter(vo -> vo.parentId() == null || vo.parentId() == ROOT_PARENT_ID
                || !existingIds.contains(vo.parentId()))
            .sorted(CATEGORY_SORT_COMPARATOR)
            .map(vo -> attachChildren(vo, childrenMap))
            .collect(Collectors.toList());
    }

    /**
     * 规范化同级顺序并交换目标与相邻节点位置。
     * <p>同级列表由调用方按 sort ASC, id ASC 查询返回（此处排序为稳定排序，保持 id 次序兜底）。
     * 上移/下移语义：先交换目标与相邻节点位置，再按 10,20,30… 规范化重排，
     * 返回排序值发生变化的实体，由调用方逐一乐观锁落库（冲突返回 409）。</p>
     *
     * @param siblings 同级分类列表（与目标同一父级，须包含目标）
     * @param target   目标分类实体
     * @param up       true-上移（与前一个节点交换） false-下移（与后一个节点交换）
     * @return 排序值发生变化的实体列表
     * @throws BusinessException 目标已处于顶部/底部无法移动时抛 10001
     */
    public List<BaseMaterialCategory> moveSibling(List<BaseMaterialCategory> siblings,
                                                  BaseMaterialCategory target, boolean up) {
        List<BaseMaterialCategory> ordered = new ArrayList<>(siblings);
        ordered.sort(Comparator.comparing(BaseMaterialCategory::getCategorySort,
            Comparator.nullsLast(Comparator.naturalOrder())));
        int index = indexOfKey(ordered, target);
        int neighborIndex = up ? index - 1 : index + 1;
        if (neighborIndex < 0 || neighborIndex >= ordered.size()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, up
                ? "已处于同级顶部，无法上移" : "已处于同级底部，无法下移");
        }
        // 交换位置后按 10,20,30… 规范化重排
        BaseMaterialCategory neighbor = ordered.get(neighborIndex);
        ordered.set(index, neighbor);
        ordered.set(neighborIndex, target);
        List<BaseMaterialCategory> changed = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            BaseMaterialCategory row = ordered.get(i);
            int expected = (i + 1) * SORT_STEP;
            if (!Objects.equals(row.getCategorySort(), expected)) {
                row.setCategorySort(expected);
                changed.add(row);
            }
        }
        return changed;
    }

    /**
     * 自底向上递归装配子树（record 不可变，需逐层新建节点）。
     */
    private static CategoryTreeVO attachChildren(CategoryTreeVO vo, Map<Long, List<CategoryTreeVO>> childrenMap) {
        List<CategoryTreeVO> children = childrenMap.get(vo.id());
        if (children == null) {
            return vo;
        }
        List<CategoryTreeVO> attached = children.stream()
            .sorted(CATEGORY_SORT_COMPARATOR)
            .map(child -> attachChildren(child, childrenMap))
            .collect(Collectors.toList());
        return new CategoryTreeVO(vo.id(), vo.categoryCode(), vo.categoryName(), vo.parentId(),
            vo.categoryLevel(), vo.isLeaf(), vo.categorySort(), vo.status(), vo.remark(), vo.version(),
            vo.materialCount(), attached);
    }

    /**
     * 按主键定位目标下标（目标实体与同级列表来自不同查询，不能用 == 同一性比较）。
     */
    private static int indexOfKey(List<BaseMaterialCategory> list, BaseMaterialCategory target) {
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), target.getId())) {
                return i;
            }
        }
        return -1;
    }
}
