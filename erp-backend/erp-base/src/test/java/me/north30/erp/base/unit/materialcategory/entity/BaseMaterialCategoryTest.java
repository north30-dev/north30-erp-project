package me.north30.erp.base.unit.materialcategory.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.unit.materialcategory.CategoryTestFactory;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link BaseMaterialCategory} 实体充血行为纯单元测试：层级推导、编码联动格式、
 * 状态值域守卫、叶子标记维护与创建默认值。
 */
class BaseMaterialCategoryTest {

    @Nested
    @DisplayName("applyParent：父级解析与层级推导")
    class ApplyParentTest {

        @Test
        @DisplayName("父级为 null，置为一级分类（parentId=0，层级 1）")
        void shouldBecomeLevel1_whenParentNull() {
            BaseMaterialCategory category = new BaseMaterialCategory();

            category.applyParent(null);

            assertThat(category.getParentId()).isZero();
            assertThat(category.getCategoryLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("父级为一级分类，推导层级 2")
        void shouldBecomeLevel2_whenParentLevel1() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory category = new BaseMaterialCategory();

            category.applyParent(parent);

            assertThat(category.getParentId()).isEqualTo(1L);
            assertThat(category.getCategoryLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("父级已是二级分类，超过两级上限，抛 10001")
        void shouldThrow_whenLevelExceedsTwo() {
            BaseMaterialCategory parent = CategoryTestFactory.level2(3L, "0101", "电阻", 1L, 10);
            BaseMaterialCategory category = new BaseMaterialCategory();

            assertThatThrownBy(() -> category.applyParent(parent))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类层级超过上限，最多两级");
        }
    }

    @Nested
    @DisplayName("applyCategoryCode：编码联动格式校验")
    class ApplyCategoryCodeTest {

        @Test
        @DisplayName("一级分类 2 位数字编码，通过并回填")
        void shouldApply_whenLevel1TwoDigits() {
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(null);

            category.applyCategoryCode("01", null);

            assertThat(category.getCategoryCode()).isEqualTo("01");
        }

        @Test
        @DisplayName("一级分类编码非 2 位数字，抛 10001")
        void shouldThrow_whenLevel1CodeNotTwoDigits() {
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(null);

            assertThatThrownBy(() -> category.applyCategoryCode("0101", null))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("一级分类编码须为 2 位数字（如 01）");
        }

        @Test
        @DisplayName("一级分类编码含非数字字符，抛 10001")
        void shouldThrow_whenLevel1CodeNotNumeric() {
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(null);

            assertThatThrownBy(() -> category.applyCategoryCode("AB", null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("一级分类编码须为 2 位数字（如 01）");
        }

        @Test
        @DisplayName("二级分类 4 位数字且以父级编码为前缀，通过")
        void shouldApply_whenLevel2CodePrefixedByParent() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(parent);

            category.applyCategoryCode("0103", parent);

            assertThat(category.getCategoryCode()).isEqualTo("0103");
        }

        @Test
        @DisplayName("二级分类编码未以父级编码为前缀，抛 10001")
        void shouldThrow_whenLevel2CodeNotPrefixedByParent() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(parent);

            assertThatThrownBy(() -> category.applyCategoryCode("0201", parent))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("二级分类编码须以父级编码开头");
        }

        @Test
        @DisplayName("二级分类编码非 4 位数字，抛 10001")
        void shouldThrow_whenLevel2CodeNotFourDigits() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(parent);

            assertThatThrownBy(() -> category.applyCategoryCode("010", parent))
                .isInstanceOf(BusinessException.class)
                .hasMessage("二级分类编码须为 4 位数字（如 0103）");
        }
    }

    @Nested
    @DisplayName("changeStatus：状态值域守卫")
    class ChangeStatusTest {

        @Test
        @DisplayName("目标状态 0/1，正常变更")
        void shouldChange_whenStatusZeroOrOne() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);

            category.changeStatus(0);
            assertThat(category.getStatus()).isZero();

            category.changeStatus(1);
            assertThat(category.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("目标状态越界（2）或为 null，抛 10001")
        void shouldThrow_whenStatusIllegal() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);

            assertThatThrownBy(() -> category.changeStatus(2))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");

            assertThatThrownBy(() -> category.changeStatus(null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
        }
    }

    @Nested
    @DisplayName("refreshLeafFlag：叶子标记维护")
    class RefreshLeafFlagTest {

        @Test
        @DisplayName("存在下级分类，叶子标记置 FALSE")
        void shouldSetFalse_whenHasChildren() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);

            category.refreshLeafFlag(true);

            assertThat(category.getIsLeaf()).isFalse();
        }

        @Test
        @DisplayName("无下级分类，叶子标记恢复 TRUE")
        void shouldSetTrue_whenNoChildren() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            category.setIsLeaf(Boolean.FALSE);

            category.refreshLeafFlag(false);

            assertThat(category.getIsLeaf()).isTrue();
        }
    }

    @Nested
    @DisplayName("applyCreateDefault：创建默认值")
    class ApplyCreateDefaultTest {

        @Test
        @DisplayName("未指定 sort/status 时补默认值，叶子标记默认 TRUE")
        void shouldApplyDefaults_whenFieldsAbsent() {
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.applyParent(null);

            category.applyCreateDefault();

            assertThat(category.getCategorySort()).isZero();
            assertThat(category.getStatus()).isEqualTo(1);
            assertThat(category.getIsLeaf()).isTrue();
        }

        @Test
        @DisplayName("显式传入值不覆盖")
        void shouldKeepExplicitValues_whenProvided() {
            BaseMaterialCategory category = new BaseMaterialCategory();
            category.setCategorySort(30);
            category.setStatus(0);

            category.applyCreateDefault();

            assertThat(category.getCategorySort()).isEqualTo(30);
            assertThat(category.getStatus()).isZero();
        }
    }
}
