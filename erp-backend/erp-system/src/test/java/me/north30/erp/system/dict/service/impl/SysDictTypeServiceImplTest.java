package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.DictTestFactory;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.converter.DictConverterImpl;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.system.support.MpTableInfoInit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysDictTypeServiceImpl} 纯单元测试：分页计数、编码唯一、乐观锁与删除约束。
 */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @Mock
    private SysDictItemMapper sysDictItemMapper;

    @Spy
    private final DictConverter dictConverter = new DictConverterImpl();

    @InjectMocks
    private SysDictTypeServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<SysDictType>> pageCaptor;

    @Captor
    private ArgumentCaptor<SysDictType> typeCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysDictType.class, SysDictItem.class);
    }

    @Nested
    @DisplayName("page：分页查询字典类型")
    class PageTest {

        @Test
        @DisplayName("分页查询有字典项的类型，返回正确分页结果")
        void shouldReturnPageWithItemCounts_whenTypesExist() {
            // Given：两条类型记录，其中 settlement_method 有 3 个字典项
            SysDictType typeA = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            typeA.setCreateTime(LocalDateTime.of(2026, 9, 1, 8, 0, 0));
            SysDictType typeB = DictTestFactory.dictType(2L, "yes_no", "是否", 0);
            Page<SysDictType> page = new Page<>(1, 20);
            page.setTotal(2);
            page.setRecords(List.of(typeA, typeB));
            given(sysDictTypeMapper.selectPage(any(), any())).willReturn(page);
            given(sysDictItemMapper.selectMaps(any()))
                .willReturn(List.of(Map.of("dict_type", "settlement_method", "cnt", 3L)));

            // When
            var result = service.page(DictTestFactory.typeQueryDTO(null, null, null, 1, 20));

            // Then：分页字段正确，无计数的类型 itemCount 归 0
            assertThat(result.total()).isEqualTo(2L);
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.pages()).isEqualTo(1L);
            List<DictTypeVO> vos = result.list();
            assertThat(vos).hasSize(2);
            assertThat(vos.get(0).dictType()).isEqualTo("settlement_method");
            assertThat(vos.get(0).itemCount()).isEqualTo(3L);
            assertThat(vos.get(0).createTime()).isEqualTo("2026-09-01 08:00:00");
            assertThat(vos.get(1).itemCount()).isZero();
        }

        @Test
        @DisplayName("空结果页不触发字典项统计查询")
        void shouldSkipItemCounts_whenNoRecords() {
            // Given：空结果页
            Page<SysDictType> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysDictTypeMapper.selectPage(any(), any())).willReturn(page);

            // When
            var result = service.page(DictTestFactory.typeQueryDTO(null, null, null, 1, 20));

            // Then：空页不触发字典项统计查询
            assertThat(result.list()).isEmpty();
            assertThat(result.total()).isZero();
            verify(sysDictItemMapper, never()).selectMaps(any());
        }

        @Test
        @DisplayName("pageSize 超过最大限制，抛出异常")
        void shouldThrow_whenPageSizeExceedsLimit() {
            // Given
            var query = DictTestFactory.typeQueryDTO(null, null, null, 1, 201);

            // When + Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("pageSize 不能超过 200");
            verifyNoInteractions(sysDictTypeMapper, sysDictItemMapper);
        }

        @Test
        @DisplayName("pageNum 小于 1 时归位为 1")
        void shouldNormalizePageNum_whenNonPositive() {
            // Given：页码小于 1 时归位为 1
            Page<SysDictType> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysDictTypeMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.page(DictTestFactory.typeQueryDTO(null, null, null, 0, 20));

            // Then
            verify(sysDictTypeMapper).selectPage(pageCaptor.capture(), any());
            assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
            assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("create：创建字典类型")
    class CreateTest {

        @Test
        @DisplayName("创建字典类型，未指定 status 时默认启用")
        void shouldInsertWithDefaults_whenTypeNotExists() {
            // Given：status 为空触发默认启用
            var dto = DictTestFactory.typeCreateDTO("settlement_method", "结算方式", null, "收付款方式");
            given(sysDictTypeMapper.selectCount(any())).willReturn(0L);
            given(sysDictTypeMapper.insert(any(SysDictType.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysDictType.class).setId(7L);
                return 1;
            });

            // When
            MutationVO vo = service.create(dto);

            // Then
            assertThat(vo.id()).isEqualTo(7L);
            assertThat(vo.updateTime()).isNull();
            assertThat(vo.isDeleted()).isNull();
            verify(sysDictTypeMapper).insert(typeCaptor.capture());
            assertThat(typeCaptor.getValue().getDictType()).isEqualTo("settlement_method");
            assertThat(typeCaptor.getValue().getDictName()).isEqualTo("结算方式");
            assertThat(typeCaptor.getValue().getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("字典类型编码已存在，抛出异常")
        void shouldThrow_whenDictTypeExists() {
            // Given
            var dto = DictTestFactory.typeCreateDTO("settlement_method", "结算方式", 1, null);
            given(sysDictTypeMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DICT_TYPE_EXISTS.getCode()))
                .hasMessage("字典类型 settlement_method 已存在");
            verify(sysDictTypeMapper, never()).insert(any(SysDictType.class));
        }
    }

    @Nested
    @DisplayName("update：更新字典类型")
    class UpdateTest {

        @Test
        @DisplayName("更新字典类型，仅更新名称/状态，remark 为空保持原值")
        void shouldUpdateFields_whenTypeExists() {
            // Given：dict_type 不可修改，仅更新名称/状态，remark 为空保持原值
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "旧名称", 1);
            type.setRemark("原备注");
            type.setUpdateTime(LocalDateTime.of(2026, 9, 28, 12, 0, 0));
            var dto = DictTestFactory.typeUpdateDTO("新名称", 0, null, 4);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(1);

            // When
            MutationVO vo = service.update(1L, dto);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 12:00:00");
            verify(sysDictTypeMapper).updateById(typeCaptor.capture());
            assertThat(typeCaptor.getValue().getDictName()).isEqualTo("新名称");
            assertThat(typeCaptor.getValue().getStatus()).isEqualTo(0);
            assertThat(typeCaptor.getValue().getRemark()).isEqualTo("原备注");
            assertThat(typeCaptor.getValue().getVersion()).isEqualTo(4);
        }

        @Test
        @DisplayName("字典类型不存在，抛出异常")
        void shouldThrow_whenTypeNotFound() {
            // Given
            given(sysDictTypeMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, DictTestFactory.typeUpdateDTO("新名称", 1, null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 1 不存在");
            verify(sysDictTypeMapper, never()).updateById(any(SysDictType.class));
        }

        @Test
        @DisplayName("更新字典类型，乐观锁冲突，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, DictTestFactory.typeUpdateDTO("结算方式", 1, null, 9)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT.getCode()))
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("delete：删除字典类型")
    class DeleteTest {

        @Test
        @DisplayName("删除字典类型，类型下无字典项")
        void shouldDelete_whenNoItems() {
            // Given：类型下无字典项
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictItemMapper.selectCount(any())).willReturn(0L);
            given(sysDictTypeMapper.deleteById(1L)).willReturn(1);

            // When
            MutationVO vo = service.delete(1L);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysDictTypeMapper).deleteById(1L);
        }

        @Test
        @DisplayName("删除字典类型，类型下存在字典项")
        void shouldThrow_whenItemsExist() {
            // Given：类型下存在字典项
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictItemMapper.selectCount(any())).willReturn(5L);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DICT_ITEM_REFERENCED.getCode()))
                .hasMessage("字典类型 settlement_method 下存在 5 个字典项，不可删除");
            verify(sysDictTypeMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("字典类型不存在，抛出异常")
        void shouldThrow_whenTypeNotFound() {
            // Given
            given(sysDictTypeMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 1 不存在");
            verifyNoInteractions(sysDictItemMapper);
        }
    }
}
