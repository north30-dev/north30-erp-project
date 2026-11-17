package me.north30.erp.base.unit.materialcategory.strategy;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.strategy.CategoryTreeStrategy;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;
import me.north30.erp.base.unit.materialcategory.CategoryTestFactory;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link CategoryTreeStrategy} 纯单元测试：树构造（排序/孤儿提升）与同级规范化重排。
 */
class CategoryTreeStrategyTest {

    private final CategoryTreeStrategy strategy = new CategoryTreeStrategy();

    @Nested
    @DisplayName("buildTree：树构造")
    class BuildTreeTest {

        @Test
        @DisplayName("空列表返回空树")
        void shouldReturnEmpty_whenNoCategories() {
            assertThat(strategy.buildTree(List.of())).isEmpty();
            assertThat(strategy.buildTree(null)).isEmpty();
        }

        @Test
        @DisplayName("两个一级分类与一个二级分类组树，同级按 categorySort 升序")
        void shouldBuildHierarchySorted_whenCategoriesExist() {
            List<CategoryTreeVO> vos = List.of(
                CategoryTestFactory.toTreeVO(CategoryTestFactory.level1(1L, "01", "电子料", 20)),
                CategoryTestFactory.toTreeVO(CategoryTestFactory.level1(2L, "02", "结构件", 10)),
                CategoryTestFactory.toTreeVO(CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10)));

            List<CategoryTreeVO> tree = strategy.buildTree(vos);

            assertThat(tree).extracting(CategoryTreeVO::id).containsExactly(2L, 1L);
            assertThat(tree.get(1).children()).extracting(CategoryTreeVO::id).containsExactly(3L);
            assertThat(tree.get(0).children()).isNull();
        }

        @Test
        @DisplayName("按条件过滤后父级不在结果集，子节点提升为根避免数据丢失")
        void shouldPromoteOrphanToRoot_whenParentFilteredOut() {
            List<CategoryTreeVO> vos = List.of(
                CategoryTestFactory.treeVO(1L, "01", "电子料", 99L, 1, Boolean.TRUE, 10, 1),
                CategoryTestFactory.toTreeVO(CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10)));

            List<CategoryTreeVO> tree = strategy.buildTree(vos);

            assertThat(tree).extracting(CategoryTreeVO::id).containsExactly(1L);
            assertThat(tree.get(0).children()).extracting(CategoryTreeVO::id).containsExactly(3L);
        }
    }

    @Nested
    @DisplayName("moveSibling：同级规范化重排")
    class MoveSiblingTest {

        @Test
        @DisplayName("上移时与前一节点交换并规范化重排，仅返回排序值变化的行")
        void shouldSwapWithNeighborAndNormalize_whenMoveUp() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 20);
            BaseMaterialCategory third = CategoryTestFactory.level1(3L, "03", "标准件", 30);

            List<BaseMaterialCategory> changed =
                strategy.moveSibling(List.of(first, target, third), target, true);

            assertThat(target.getCategorySort()).isEqualTo(10);
            assertThat(first.getCategorySort()).isEqualTo(20);
            assertThat(third.getCategorySort()).isEqualTo(30);
            assertThat(changed).extracting(BaseMaterialCategory::getId).containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("下移时与后一节点交换并规范化重排")
        void shouldSwapWithNeighborAndNormalize_whenMoveDown() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 20);
            BaseMaterialCategory third = CategoryTestFactory.level1(3L, "03", "标准件", 30);

            List<BaseMaterialCategory> changed =
                strategy.moveSibling(List.of(first, target, third), target, false);

            assertThat(target.getCategorySort()).isEqualTo(30);
            assertThat(third.getCategorySort()).isEqualTo(20);
            assertThat(first.getCategorySort()).isEqualTo(10);
            // 变更行按规范化重排后的遍历序返回（first 未变化，third/target 相邻交换后均变化）
            assertThat(changed).extracting(BaseMaterialCategory::getId).containsExactly(3L, 2L);
        }

        @Test
        @DisplayName("既有排序值不规范（5,10,30）时按 10,20,30… 规范化，仅变化行返回")
        void shouldNormalizeDirtySorts_whenMoveSibling() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 5);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 10);
            BaseMaterialCategory third = CategoryTestFactory.level1(3L, "03", "标准件", 30);

            List<BaseMaterialCategory> changed =
                strategy.moveSibling(List.of(first, target, third), target, true);

            // 交换后 [target, first, third] → 10,20,30；target 原排序即 10 未变，first 5→20 变化，third 保持 30 未变
            assertThat(target.getCategorySort()).isEqualTo(10);
            assertThat(first.getCategorySort()).isEqualTo(20);
            assertThat(third.getCategorySort()).isEqualTo(30);
            assertThat(changed).extracting(BaseMaterialCategory::getId).containsExactly(1L);
        }

        @Test
        @DisplayName("已处于同级顶部时上移抛 10001")
        void shouldThrow_whenMoveUpAtTop() {
            BaseMaterialCategory target = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory second = CategoryTestFactory.level1(2L, "02", "结构件", 20);

            assertThatThrownBy(() -> strategy.moveSibling(List.of(target, second), target, true))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("已处于同级顶部，无法上移");
        }

        @Test
        @DisplayName("已处于同级底部时下移抛 10001")
        void shouldThrow_whenMoveDownAtBottom() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 20);

            assertThatThrownBy(() -> strategy.moveSibling(List.of(first, target), target, false))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("已处于同级底部，无法下移");
        }
    }
}
