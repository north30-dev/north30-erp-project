package me.north30.erp.system.common.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

/**
 * 同级排序工具（08 文档阶段四排序统一：树/有序集合保留 sort，交互走 move 接口）。
 * <p>上移/下移语义：先对同级按 sort 升序做规范化重排（10,20,30…），再交换目标与相邻节点位置，
 * 返回排序值发生变化的实体，由调用方逐一乐观锁落库（冲突返回 409）。</p>
 */
public final class SiblingSortUtil {

    /** 规范化重排的排序步长 */
    private static final int SORT_STEP = 10;

    private SiblingSortUtil() {
    }

    /**
     * 规范化同级顺序并交换目标与相邻节点位置。
     * <p>同级列表已由调用方按 sort ASC, id ASC 查询返回（此处排序为稳定排序，保持 id 次序兜底）。</p>
     *
     * @param siblings   同级实体列表（与目标同一父级/分组，非空且必须包含目标）
     * @param target     目标实体（与 siblings 同库不同查询，须按主键定位而非对象同一性）
     * @param up         true-上移（与前一个节点交换） false-下移（与后一个节点交换）
     * @param keyGetter  主键读取（定位目标在同级列表中的位置）
     * @param sortGetter 排序字段读取
     * @param sortSetter 排序字段写入
     * @param <T>        实体类型
     * @return 排序值发生变化的实体列表，调用方逐一 updateById 落库并校验乐观锁
     * @throws me.north30.erp.common.exception.BusinessException 目标已处于顶部/底部无法移动时抛 10001
     */
    public static <T> List<T> moveSibling(List<T> siblings, T target, boolean up,
                                          Function<T, ?> keyGetter, Function<T, Integer> sortGetter,
                                          BiConsumer<T, Integer> sortSetter) {
        List<T> ordered = new ArrayList<>(siblings);
        ordered.sort(Comparator.comparing(sortGetter, Comparator.nullsLast(Comparator.naturalOrder())));
        int index = indexOfKey(ordered, target, keyGetter);
        int neighborIndex = up ? index - 1 : index + 1;
        if (neighborIndex < 0 || neighborIndex >= ordered.size()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, up
                ? "已处于同级顶部，无法上移" : "已处于同级底部，无法下移");
        }
        // 交换位置后按 10,20,30… 规范化重排
        T neighbor = ordered.get(neighborIndex);
        ordered.set(index, neighbor);
        ordered.set(neighborIndex, target);
        List<T> changed = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            T row = ordered.get(i);
            int expected = (i + 1) * SORT_STEP;
            if (!Objects.equals(sortGetter.apply(row), expected)) {
                sortSetter.accept(row, expected);
                changed.add(row);
            }
        }
        return changed;
    }

    /** 按主键定位目标下标（目标实体与同级列表来自不同查询，不能用 == 同一性比较）。 */
    private static <T> int indexOfKey(List<T> list, T target, Function<T, ?> keyGetter) {
        Object targetKey = keyGetter.apply(target);
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(keyGetter.apply(list.get(i)), targetKey)) {
                return i;
            }
        }
        return -1;
    }
}
