package me.north30.erp.base.unit.materialcategory.service.impl;

import java.time.LocalDateTime;
import java.util.List;

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

import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.materialcategory.converter.CategoryConverter;
import me.north30.erp.base.materialcategory.converter.CategoryConverterImpl;
import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryMoveDTO;
import me.north30.erp.base.materialcategory.dto.CategoryStatusDTO;
import me.north30.erp.base.materialcategory.dto.CategoryTreeQueryDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.base.materialcategory.service.impl.MaterialCategoryApplicationServiceImpl;
import me.north30.erp.base.materialcategory.strategy.CategoryTreeStrategy;
import me.north30.erp.base.materialcategory.vo.CategoryMutationVO;
import me.north30.erp.base.materialcategory.vo.CategoryStatusVO;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;
import me.north30.erp.base.unit.materialcategory.CategoryTestFactory;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * {@link MaterialCategoryApplicationServiceImpl} 纯单元测试：树查询编排、新增编排
 * （编码唯一/层级上限/编码联动/父级叶子刷新）、修改（Converter 部分更新）、
 * 删除编排（挂载物料计数经物料域 DomainService 取得后传参）、状态变更、move 委派。
 * <p>Converter 使用 MapStruct 生成实现（{@link CategoryConverterImpl} 真实实例），
 * 保证映射路径与生产一致。</p>
 */
@ExtendWith(MockitoExtension.class)
class MaterialCategoryApplicationServiceImplTest {

    @Mock
    private MaterialCategoryDomainService materialCategoryDomainService;

    @Mock
    private MaterialDomainService materialDomainService;

    private final CategoryConverter categoryConverter = new CategoryConverterImpl();

    private MaterialCategoryApplicationServiceImpl service;

    @Captor
    private ArgumentCaptor<BaseMaterialCategory> categoryCaptor;

    @BeforeEach
    void setUp() {
        service = new MaterialCategoryApplicationServiceImpl(materialCategoryDomainService,
            new CategoryTreeStrategy(), categoryConverter, materialDomainService);
    }

    @Nested
    @DisplayName("listTree：分类树查询编排")
    class ListTreeTest {

        @Test
        @DisplayName("查询条件透传 DomainService，实体经 Converter 转 VO 后由策略组树")
        void shouldBuildTree_whenCategoriesExist() {
            BaseMaterialCategory root = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            BaseMaterialCategory child = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(materialCategoryDomainService.listCategories("电子料", null, null))
                .willReturn(List.of(root, child));

            List<CategoryTreeVO> tree = service.listTree(new CategoryTreeQueryDTO("电子料", null, null));

            assertThat(tree).extracting(CategoryTreeVO::id).containsExactly(1L);
            assertThat(tree.get(0).children()).extracting(CategoryTreeVO::id).containsExactly(3L);
        }

        @Test
        @DisplayName("无分类时返回空树")
        void shouldReturnEmpty_whenNoCategories() {
            given(materialCategoryDomainService.listCategories(any(), any(), any())).willReturn(List.of());

            assertThat(service.listTree(CategoryTestFactory.treeQueryDTO())).isEmpty();
        }
    }

    @Nested
    @DisplayName("create：新增分类编排")
    class CreateTest {

        @Test
        @DisplayName("新增一级分类：默认状态启用、sort 取同级 max+10、叶子标记 TRUE，不刷新父级")
        void shouldCreateLevel1WithDefaults_whenParentIdZero() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("09", "辅料", 0L, null, null);
            given(materialCategoryDomainService.nextCategorySort(0L)).willReturn(10);
            willAnswer(invocation -> {
                invocation.getArgument(0, BaseMaterialCategory.class).setId(10L);
                invocation.getArgument(0, BaseMaterialCategory.class)
                    .setUpdateTime(LocalDateTime.of(2026, 11, 16, 12, 0, 0));
                return null;
            }).given(materialCategoryDomainService).insert(any(BaseMaterialCategory.class));

            CategoryMutationVO vo = service.create(dto);

            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.categoryLevel()).isEqualTo(1);
            assertThat(vo.isLeaf()).isTrue();
            assertThat(vo.updateTime()).isEqualTo("2026-11-16 12:00:00");
            verify(materialCategoryDomainService).insert(categoryCaptor.capture());
            BaseMaterialCategory inserted = categoryCaptor.getValue();
            assertThat(inserted.getCategoryCode()).isEqualTo("09");
            assertThat(inserted.getParentId()).isZero();
            assertThat(inserted.getCategorySort()).isEqualTo(10);
            assertThat(inserted.getStatus()).isEqualTo(1);
            verify(materialCategoryDomainService).refreshParentLeafFlag(0L);
        }

        @Test
        @DisplayName("新增二级分类：层级与父级 ID 正确，新增后刷新父级叶子标记")
        void shouldCreateLevel2AndRefreshParentLeaf_whenParentLevel1() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("0103", "芯片", 1L, 30, 1);
            given(materialCategoryDomainService.requireParentCategory(1L))
                .willReturn(CategoryTestFactory.level1(1L, "01", "电子料", 10));
            willAnswer(invocation -> {
                invocation.getArgument(0, BaseMaterialCategory.class).setId(11L);
                return null;
            }).given(materialCategoryDomainService).insert(any(BaseMaterialCategory.class));

            CategoryMutationVO vo = service.create(dto);

            assertThat(vo.id()).isEqualTo(11L);
            assertThat(vo.categoryLevel()).isEqualTo(2);
            verify(materialCategoryDomainService).insert(categoryCaptor.capture());
            assertThat(categoryCaptor.getValue().getParentId()).isEqualTo(1L);
            assertThat(categoryCaptor.getValue().getCategorySort()).isEqualTo(30);
            verify(materialCategoryDomainService).refreshParentLeafFlag(1L);
        }

        @Test
        @DisplayName("编码已存在，抛 11007，不落库")
        void shouldThrow_whenCodeExists() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("01", "电子料", 0L, null, null);
            willThrow(new BusinessException(me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_CODE_EXISTS,
                "分类编码 01 已存在"))
                .given(materialCategoryDomainService).requireCodeAvailable("01");

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode())
                        .isEqualTo(me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_CODE_EXISTS.getCode()))
                .hasMessage("分类编码 01 已存在");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("父级分类不存在，抛 10001，不落库")
        void shouldThrow_whenParentMissing() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("0103", "芯片", 99L, null, null);
            given(materialCategoryDomainService.requireParentCategory(99L))
                .willThrow(new BusinessException(CommonErrorCode.PARAM_ERROR, "物料分类 99 不存在"));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类 99 不存在");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("父级已是二级分类，超过两级层级上限，抛 10001")
        void shouldThrow_whenLevelExceedsTwo() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("010301", "芯片组", 3L, null, null);
            given(materialCategoryDomainService.requireParentCategory(3L))
                .willReturn(CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("物料分类层级超过上限，最多两级");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("二级编码未以父级编码为前缀，联动校验抛 10001")
        void shouldThrow_whenLevel2CodeNotPrefixedByParent() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("0201", "芯片", 1L, null, null);
            given(materialCategoryDomainService.requireParentCategory(1L))
                .willReturn(CategoryTestFactory.level1(1L, "01", "电子料", 10));

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("二级分类编码须以父级编码开头");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("一级编码格式非法（非 2 位数字），抛 10001")
        void shouldThrow_whenLevel1CodeIllegal() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("AB", "电子料", 0L, null, null);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("一级分类编码须为 2 位数字（如 01）");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("显式状态非法（2），值域守卫抛 10001")
        void shouldThrow_whenStatusIllegal() {
            CategoryCreateDTO dto = CategoryTestFactory.createDTO("01", "电子料", 0L, null, 2);

            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
            verify(materialCategoryDomainService, never()).insert(any(BaseMaterialCategory.class));
        }
    }

    @Nested
    @DisplayName("update：修改分类编排（Converter 部分更新）")
    class UpdateTest {

        @Test
        @DisplayName("选填字段非 null 才应用，携带乐观锁版本落库，编码与父级不拷贝")
        void shouldApplyNonNullFields_whenUpdate() {
            BaseMaterialCategory current = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(materialCategoryDomainService.requireCategory(1L)).willReturn(current);

            CategoryMutationVO vo = service.update(1L, CategoryTestFactory.updateDTO("电子元器件", 20, 0, 3));

            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.updateTime()).isNotBlank();
            verify(materialCategoryDomainService).updateRow(categoryCaptor.capture());
            BaseMaterialCategory updated = categoryCaptor.getValue();
            assertThat(updated.getCategoryName()).isEqualTo("电子元器件");
            assertThat(updated.getCategorySort()).isEqualTo(20);
            assertThat(updated.getStatus()).isZero();
            assertThat(updated.getVersion()).isEqualTo(3);
            // 编码与父级不可修改（Converter 无对应映射即不拷贝）
            assertThat(updated.getCategoryCode()).isNull();
            assertThat(updated.getParentId()).isNull();
        }

        @Test
        @DisplayName("字段为 null 表示不修改（Converter IGNORE 策略，patch 字段保持 null）")
        void shouldKeepFields_whenDtoFieldsNull() {
            BaseMaterialCategory current = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(materialCategoryDomainService.requireCategory(1L)).willReturn(current);

            service.update(1L, CategoryTestFactory.updateDTO(null, null, null, 2));

            verify(materialCategoryDomainService).updateRow(categoryCaptor.capture());
            BaseMaterialCategory patch = categoryCaptor.getValue();
            assertThat(patch.getCategoryName()).isNull();
            assertThat(patch.getCategorySort()).isNull();
            assertThat(patch.getStatus()).isNull();
            assertThat(patch.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("乐观锁冲突（更新 0 行），抛 10601")
        void shouldThrow_whenOptimisticLockConflict() {
            given(materialCategoryDomainService.requireCategory(1L))
                .willReturn(CategoryTestFactory.level1(1L, "01", "电子料", 10));
            willThrow(new OptimisticLockingFailureException("数据已被其他操作修改，请重试"))
                .given(materialCategoryDomainService).updateRow(any(BaseMaterialCategory.class));

            assertThatThrownBy(() -> service.update(1L, CategoryTestFactory.updateDTO("电子料", null, null, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("delete：删除分类编排（挂载物料计数经物料域 DomainService 取得）")
    class DeleteTest {

        @Test
        @DisplayName("无下级且无挂载物料，删除成功并刷新父级叶子标记")
        void shouldDelete_whenDeletable() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(materialCategoryDomainService.requireCategory(3L)).willReturn(category);
            given(materialDomainService.countByCategoryId(3L)).willReturn(0L);

            CategoryMutationVO vo = service.delete(3L);

            assertThat(vo.id()).isEqualTo(3L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(materialCategoryDomainService).requireDeletable(category, 0L);
            verify(materialCategoryDomainService).deleteById(3L);
            verify(materialCategoryDomainService).refreshParentLeafFlag(1L);
        }

        @Test
        @DisplayName("存在下级分类，删除校验抛 11006，不删除")
        void shouldThrow_whenHasChildren() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(materialCategoryDomainService.requireCategory(1L)).willReturn(category);
            given(materialDomainService.countByCategoryId(1L)).willReturn(0L);
            willThrow(new BusinessException(me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_HAS_CHILDREN,
                "分类 电子料 存在下级分类，不可删除"))
                .given(materialCategoryDomainService).requireDeletable(category, 0L);

            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(
                        me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_HAS_CHILDREN.getCode()))
                .hasMessage("分类 电子料 存在下级分类，不可删除");
            verify(materialCategoryDomainService, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("已挂载物料（物料域计数 > 0），删除校验抛 11006，不删除")
        void shouldThrow_whenMaterialsMounted() {
            BaseMaterialCategory category = CategoryTestFactory.level2(3L, "0103", "芯片", 1L, 10);
            given(materialCategoryDomainService.requireCategory(3L)).willReturn(category);
            given(materialDomainService.countByCategoryId(3L)).willReturn(5L);
            willThrow(new BusinessException(me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_HAS_CHILDREN,
                "分类 芯片 已挂载物料，不可删除"))
                .given(materialCategoryDomainService).requireDeletable(category, 5L);

            assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(
                        me.north30.erp.base.common.enums.BaseErrorCode.CATEGORY_HAS_CHILDREN.getCode()))
                .hasMessage("分类 芯片 已挂载物料，不可删除");
            verify(materialCategoryDomainService, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("changeStatus：状态变更编排")
    class ChangeStatusTest {

        @Test
        @DisplayName("状态变更仅更新 status 与乐观锁版本（patch 实体）")
        void shouldPatchStatusAndVersion_whenChangeStatus() {
            BaseMaterialCategory category = CategoryTestFactory.level1(1L, "01", "电子料", 10);
            given(materialCategoryDomainService.requireCategory(1L)).willReturn(category);

            CategoryStatusVO vo = service.changeStatus(1L, CategoryTestFactory.statusDTO(0, 2));

            assertThat(vo.status()).isZero();
            verify(materialCategoryDomainService).updateRow(categoryCaptor.capture());
            BaseMaterialCategory patch = categoryCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getStatus()).isZero();
            assertThat(patch.getVersion()).isEqualTo(2);
            assertThat(patch.getCategoryCode()).isNull();
        }

        @Test
        @DisplayName("状态值非法（2），抛 10001，不落库")
        void shouldThrow_whenStatusIllegal() {
            given(materialCategoryDomainService.requireCategory(1L))
                .willReturn(CategoryTestFactory.level1(1L, "01", "电子料", 10));

            assertThatThrownBy(() -> service.changeStatus(1L, CategoryTestFactory.statusDTO(2, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
            verify(materialCategoryDomainService, never()).updateRow(any(BaseMaterialCategory.class));
        }

        @Test
        @DisplayName("乐观锁冲突（更新 0 行），抛 10601")
        void shouldThrow_whenOptimisticLockConflict() {
            given(materialCategoryDomainService.requireCategory(1L))
                .willReturn(CategoryTestFactory.level1(1L, "01", "电子料", 10));
            willThrow(new OptimisticLockingFailureException("数据已被其他操作修改，请重试"))
                .given(materialCategoryDomainService).updateRow(any(BaseMaterialCategory.class));

            assertThatThrownBy(() -> service.changeStatus(1L, CategoryTestFactory.statusDTO(1, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("move：上移/下移委派")
    class MoveTest {

        @Test
        @DisplayName("direction=up 委派为上移")
        void shouldDelegateUp_whenDirectionUp() {
            service.move(2L, new CategoryMoveDTO("up"));

            verify(materialCategoryDomainService).moveCategory(2L, true);
        }

        @Test
        @DisplayName("direction=down 委派为下移")
        void shouldDelegateDown_whenDirectionDown() {
            service.move(2L, new CategoryMoveDTO("down"));

            verify(materialCategoryDomainService).moveCategory(2L, false);
        }
    }
}
