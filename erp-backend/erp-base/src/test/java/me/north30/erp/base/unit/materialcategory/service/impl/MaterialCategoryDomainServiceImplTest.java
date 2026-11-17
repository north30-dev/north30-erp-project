package me.north30.erp.base.unit.materialcategory.service.impl;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.mapper.BaseMaterialCategoryMapper;
import me.north30.erp.base.materialcategory.service.impl.MaterialCategoryDomainServiceImpl;
import me.north30.erp.base.materialcategory.strategy.CategoryTreeStrategy;
import me.north30.erp.base.unit.materialcategory.CategoryTestFactory;
import me.north30.erp.base.unit.support.MpTableInfoInit;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link MaterialCategoryDomainServiceImpl} 纯单元测试：分类存在性、编码唯一、
 * 删除校验（子节点与挂载物料两分支）、启用分类编码查询（11017）、叶子标记维护与 move 重排。
 */
@ExtendWith(MockitoExtension.class)
class MaterialCategoryDomainServiceImplTest {

    @Mock
    private BaseMaterialCategoryMapper baseMaterialCategoryMapper;

    private MaterialCategoryDomainServiceImpl service;

    @Captor
    private ArgumentCaptor<BaseMaterialCategory> categoryCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(BaseMaterialCategory.class);
    }

    @BeforeEach
    void setUp() {
        service = new MaterialCategoryDomainServiceImpl(baseMaterialCategoryMapper, new CategoryTreeStrategy());
    }

    @Nested
    @DisplayName("requireCategory：分类存在性校验")
    class RequireCategoryTest {

        @Test
        @DisplayName("分类不存在，抛 10001")
        void shouldThrow_whenCategoryMissing() {
            given(baseMaterialCategoryMapper.selectById(99L)).willReturn(null);

            assertThatThrownBy(() -> service.requireCategory(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类 99 不存在");
        }

        @Test
        @DisplayName("分类存在，返回实体")
        void shouldReturn_whenCategoryExists() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(baseMaterialCategoryMapper.selectById(1L)).willReturn(category);

            assertThat(service.requireCategory(1L)).isSameAs(category);
        }
    }

    @Nested
    @DisplayName("requireCodeAvailable：编码唯一校验")
    class RequireCodeAvailableTest {

        @Test
        @DisplayName("编码已存在，抛 11007")
        void shouldThrow_whenCodeExists() {
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(1L);

            assertThatThrownBy(() -> service.requireCodeAvailable("01"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.CATEGORY_CODE_EXISTS.getCode()))
                .hasMessage("分类编码 01 已存在");
        }

        @Test
        @DisplayName("编码未占用，通过")
        void shouldPass_whenCodeAvailable() {
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(0L);

            assertThatCode(() -> service.requireCodeAvailable("09")).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("requireDeletable：删除前置校验")
    class RequireDeletableTest {

        @Test
        @DisplayName("存在下级分类，抛 11006（子节点分支）")
        void shouldThrow_whenHasChildren() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(2L);

            assertThatThrownBy(() -> service.requireDeletable(category, 0L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.CATEGORY_HAS_CHILDREN.getCode()))
                .hasMessage("分类 电子料 存在下级分类，不可删除");
        }

        @Test
        @DisplayName("无下级但已挂载物料，抛 11006（物料计数由应用服务层传入）")
        void shouldThrow_whenMaterialsMounted() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(0L);

            assertThatThrownBy(() -> service.requireDeletable(category, 5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.CATEGORY_HAS_CHILDREN.getCode()))
                .hasMessage("分类 芯片 已挂载物料，不可删除");
        }

        @Test
        @DisplayName("无下级且无挂载物料，通过删除校验")
        void shouldPass_whenNoChildrenAndNoMaterials() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(0L);

            assertThatCode(() -> service.requireDeletable(category, 0L)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("isLeaf：叶子分类查询（跨域最小接口）")
    class IsLeafTest {

        @Test
        @DisplayName("叶子分类返回 TRUE")
        void shouldReturnTrue_whenLeaf() {
            given(baseMaterialCategoryMapper.selectById(3L))
                .willReturn(CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10));

            assertThat(service.isLeaf(3L)).isTrue();
        }

        @Test
        @DisplayName("非叶子分类返回 FALSE")
        void shouldReturnFalse_whenNotLeaf() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            parent.setIsLeaf(Boolean.FALSE);
            given(baseMaterialCategoryMapper.selectById(1L)).willReturn(parent);

            assertThat(service.isLeaf(1L)).isFalse();
        }

        @Test
        @DisplayName("分类不存在，抛 10001")
        void shouldThrow_whenCategoryMissing() {
            given(baseMaterialCategoryMapper.selectById(99L)).willReturn(null);

            assertThatThrownBy(() -> service.isLeaf(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类 99 不存在");
        }
    }

    @Nested
    @DisplayName("requireEnabledCategoryCode：启用分类编码查询（跨域最小接口）")
    class RequireEnabledCategoryCodeTest {

        @Test
        @DisplayName("分类启用，返回编码")
        void shouldReturnCode_whenEnabled() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(baseMaterialCategoryMapper.selectById(3L)).willReturn(category);

            assertThat(service.requireEnabledCategoryCode(3L)).isEqualTo("0103");
        }

        @Test
        @DisplayName("分类已停用，抛 11017")
        void shouldThrow_whenDisabled() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            category.setStatus(0);
            given(baseMaterialCategoryMapper.selectById(3L)).willReturn(category);

            assertThatThrownBy(() -> service.requireEnabledCategoryCode(3L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(BaseErrorCode.MATERIAL_CATEGORY_DISABLED.getCode()))
                .hasMessage("物料分类 芯片 已停用");
        }

        @Test
        @DisplayName("分类不存在，抛 10001")
        void shouldThrow_whenCategoryMissing() {
            given(baseMaterialCategoryMapper.selectById(99L)).willReturn(null);

            assertThatThrownBy(() -> service.requireEnabledCategoryCode(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类 99 不存在");
        }
    }

    @Nested
    @DisplayName("nextCategorySort：同级下一个显示顺序")
    class NextCategorySortTest {

        @Test
        @DisplayName("同级已有排序值时取 max+10")
        void shouldReturnMaxPlusTen_whenSiblingsExist() {
            given(baseMaterialCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .willReturn(List.of(
                    CategoryTestFactory.level1(1L, "01", "电子料", 10),
                    CategoryTestFactory.level1(2L, "02", "结构件", 30)));

            assertThat(service.nextCategorySort(0L)).isEqualTo(40);
        }

        @Test
        @DisplayName("空同级返回首个步长 10")
        void shouldReturnTen_whenNoSiblings() {
            given(baseMaterialCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .willReturn(List.of());

            assertThat(service.nextCategorySort(0L)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("refreshParentLeafFlag：父级叶子标记维护")
    class RefreshParentLeafFlagTest {

        @Test
        @DisplayName("顶级父级（null/0）直接跳过，不查库")
        void shouldSkip_whenParentRoot() {
            service.refreshParentLeafFlag(null);
            service.refreshParentLeafFlag(0L);

            verify(baseMaterialCategoryMapper, never()).selectById(any());
            verify(baseMaterialCategoryMapper, never()).updateById(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("父级存在下级分类，叶子标记置 FALSE 并落库")
        void shouldSetParentLeafFalse_whenChildrenExist() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(baseMaterialCategoryMapper.selectById(1L)).willReturn(parent);
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(1L);

            service.refreshParentLeafFlag(1L);

            assertThat(parent.getIsLeaf()).isFalse();
            verify(baseMaterialCategoryMapper).updateById(parent);
        }

        @Test
        @DisplayName("父级无下级分类，叶子标记恢复 TRUE 并落库")
        void shouldRestoreParentLeafTrue_whenNoChildren() {
            BaseMaterialCategory parent = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            parent.setIsLeaf(Boolean.FALSE);
            given(baseMaterialCategoryMapper.selectById(1L)).willReturn(parent);
            given(baseMaterialCategoryMapper.selectCount(any())).willReturn(0L);

            service.refreshParentLeafFlag(1L);

            assertThat(parent.getIsLeaf()).isTrue();
            verify(baseMaterialCategoryMapper).updateById(parent);
        }
    }

    @Nested
    @DisplayName("updateRow/deleteById：落库与乐观锁")
    class PersistTest {

        @Test
        @DisplayName("更新 0 行视为乐观锁冲突，抛 10601（HTTP 409）")
        void shouldThrow_whenOptimisticLockConflict() {
            given(baseMaterialCategoryMapper.updateById(any(BaseMaterialCategory.class))).willReturn(0);

            assertThatThrownBy(() -> service.updateRow(CategoryTestFactory.level1(1L, "01", "电子料", 10)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }

        @Test
        @DisplayName("更新命中行，正常落库")
        void shouldUpdate_whenRowMatched() {
            given(baseMaterialCategoryMapper.updateById(any(BaseMaterialCategory.class))).willReturn(1);

            assertThatCode(() -> service.updateRow(CategoryTestFactory.level1(1L, "01", "电子料", 10)))
                .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("deleteById 透传逻辑删除")
        void shouldDeleteById() {
            service.deleteById(1L);

            verify(baseMaterialCategoryMapper).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("moveCategory：同级上移/下移重排")
    class MoveCategoryTest {

        @Test
        @DisplayName("上移时交换相邻节点并规范化重排，仅变更行逐一乐观锁落库")
        void shouldReorderSiblings_whenMoveUp() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 20);
            BaseMaterialCategory third = CategoryTestFactory.level1(3L, "03", "标准件", 30);
            given(baseMaterialCategoryMapper.selectById(2L)).willReturn(target);
            given(baseMaterialCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .willReturn(List.of(first, target, third));
            given(baseMaterialCategoryMapper.updateById(any(BaseMaterialCategory.class))).willReturn(1);

            service.moveCategory(2L, true);

            assertThat(target.getCategorySort()).isEqualTo(10);
            assertThat(first.getCategorySort()).isEqualTo(20);
            assertThat(third.getCategorySort()).isEqualTo(30);
            verify(baseMaterialCategoryMapper, times(2)).updateById(categoryCaptor.capture());
            assertThat(categoryCaptor.getAllValues())
                .extracting(BaseMaterialCategory::getId)
                .containsExactly(2L, 1L);
        }

        @Test
        @DisplayName("已处于同级底部时下移抛 10001，不落库")
        void shouldThrow_whenMoveDownAtBottom() {
            BaseMaterialCategory first = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory target = CategoryTestFactory.level1(2L, "02", "结构件", 20);
            given(baseMaterialCategoryMapper.selectById(2L)).willReturn(target);
            given(baseMaterialCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .willReturn(List.of(first, target));

            assertThatThrownBy(() -> service.moveCategory(2L, false))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("已处于同级底部，无法下移");
            verify(baseMaterialCategoryMapper, never()).updateById(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("落库乐观锁冲突（更新 0 行），抛 10601")
        void shouldThrow_whenOptimisticLockConflict() {
            BaseMaterialCategory target = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory second = CategoryTestFactory.level1(2L, "02", "结构件", 20);
            given(baseMaterialCategoryMapper.selectById(1L)).willReturn(target);
            given(baseMaterialCategoryMapper.selectList(any(LambdaQueryWrapper.class)))
                .willReturn(List.of(target, second));
            given(baseMaterialCategoryMapper.updateById(any(BaseMaterialCategory.class))).willReturn(0);

            assertThatThrownBy(() -> service.moveCategory(1L, false))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }
}
