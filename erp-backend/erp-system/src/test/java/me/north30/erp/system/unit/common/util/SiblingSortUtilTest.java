package me.north30.erp.system.unit.common.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.util.SiblingSortUtil;

/**
 * {@link SiblingSortUtil} 纯单元测试：规范化重排、相邻交换与顶/底边界。
 */
@DisplayName("同级排序工具")
class SiblingSortUtilTest {

    /** 测试用行对象（仅承载 id 与 sort，模拟实体最小行为） */
    private static class Row {
        final long id;
        Integer sort;

        Row(long id, Integer sort) {
            this.id = id;
            this.sort = sort;
        }
    }

    private static List<Row> rows(int... sorts) {
        List<Row> list = new ArrayList<>();
        for (int i = 0; i < sorts.length; i++) {
            list.add(new Row(i + 1, sorts[i]));
        }
        return list;
    }

    @Test
    @DisplayName("上移时与前一节点交换并规范化重排，仅返回 sort 变化的行")
    void shouldSwapAndNormalize_whenMoveUp() {
        // Given：同级 3 行 sort=10/20/30，目标为第二行
        List<Row> siblings = rows(10, 20, 30);
        Row target = siblings.get(1);

        // When
        List<Row> changed = SiblingSortUtil.moveSibling(siblings, target, true, r -> r.id, r -> r.sort, (r, s) -> r.sort = s);

        // Then：目标 id2 变 10，id1 变 20，id3 不变；变更行按规范化后顺序返回
        assertThat(siblings).extracting(r -> r.sort).containsExactly(20, 10, 30);
        assertThat(changed).extracting(r -> r.id).containsExactly(2L, 1L);
        assertThat(changed).extracting(r -> r.sort).containsExactly(10, 20);
    }

    @Test
    @DisplayName("下移时与后一节点交换")
    void shouldSwapWithNext_whenMoveDown() {
        // Given：目标为第一行
        List<Row> siblings = rows(10, 20, 30);
        Row target = siblings.get(0);

        // When
        List<Row> changed = SiblingSortUtil.moveSibling(siblings, target, false, r -> r.id, r -> r.sort, (r, s) -> r.sort = s);

        // Then：目标 id1 下移变 20，id2 上位变 10；变更行按规范化后顺序返回
        assertThat(siblings).extracting(r -> r.sort).containsExactly(20, 10, 30);
        assertThat(changed).extracting(r -> r.id).containsExactly(2L, 1L);
        assertThat(changed).extracting(r -> r.sort).containsExactly(10, 20);
    }

    @Test
    @DisplayName("sort 乱序时先规范化再交换，空档与重复值被抹平")
    void shouldNormalizeGaps_whenSortSparse() {
        // Given：乱序 sort=5/5/100
        List<Row> siblings = rows(5, 5, 100);
        Row target = siblings.get(2);

        // When：最后一行下移 → 10001
        assertThatThrownBy(() -> SiblingSortUtil.moveSibling(siblings, target, false, r -> r.id, r -> r.sort, (r, s) -> r.sort = s))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
            .hasMessage("已处于同级底部，无法下移");

        // When：第二行上移 → 与第一行交换，规范化为 10,20,30
        Row middle = siblings.get(1);
        List<Row> changed = SiblingSortUtil.moveSibling(siblings, middle, true, r -> r.id, r -> r.sort, (r, s) -> r.sort = s);

        // Then：三行 sort 全部被重排，全部计入变更
        assertThat(siblings).extracting(r -> r.sort).containsExactly(20, 10, 30);
        assertThat(changed).hasSize(3);
    }

    @Test
    @DisplayName("已处于顶部时上移抛 10001")
    void shouldThrow10001_whenMoveUpAtTop() {
        // Given
        List<Row> siblings = rows(10, 20);
        Row target = siblings.get(0);

        // When + Then
        assertThatThrownBy(() -> SiblingSortUtil.moveSibling(siblings, target, true, r -> r.id, r -> r.sort, (r, s) -> r.sort = s))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
            .hasMessage("已处于同级顶部，无法上移");
    }

    @Test
    @DisplayName("仅一行时上移/下移均抛 10001")
    void shouldThrow10001_whenSingleRow() {
        // Given
        List<Row> siblings = rows(10);
        Row target = siblings.get(0);

        // When + Then
        assertThatThrownBy(() -> SiblingSortUtil.moveSibling(siblings, target, true, r -> r.id, r -> r.sort, (r, s) -> r.sort = s))
            .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> SiblingSortUtil.moveSibling(siblings, target, false, r -> r.id, r -> r.sort, (r, s) -> r.sort = s))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("null sort 排最后，可与尾节点交换")
    void shouldTreatNullSortAsLast() {
        // Given：第二行 sort 为 null
        List<Row> siblings = rows(10);
        siblings.add(new Row(2, null));
        Row target = siblings.get(1);

        // When
        List<Row> changed = SiblingSortUtil.moveSibling(siblings, target, true, r -> r.id, r -> r.sort, (r, s) -> r.sort = s);

        // Then：null 行上移到第一位，规范化 10,20
        assertThat(changed).extracting(r -> r.id).containsExactly(2L, 1L);
        assertThat(changed).extracting(r -> r.sort).containsExactly(10, 20);
        assertThat(changed).hasSize(2);
    }
}
