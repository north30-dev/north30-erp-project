package me.north30.erp.system.unit.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.unit.dict.DictTestFactory;
import me.north30.erp.system.unit.security.support.MpTableInfoInit;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.converter.DictConverterImpl;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.service.impl.SysDictTypeServiceImpl;
import me.north30.erp.system.dict.vo.DictTypeVO;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import me.north30.erp.system.common.util.PageParamResolver;
import me.north30.erp.system.config.service.SysConfigDomainService;

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
 * {@link SysDictTypeServiceImpl} 纯单元测试：类型侧 4 个管理端用例
 * （分页计数、编码唯一、乐观锁、删除约束）与守卫方法（requireDictTypeAvailable / requireById）。
 */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    @Mock
    private SysDictItemMapper sysDictItemMapper;

    @Spy
    private final DictConverter dictConverter = new DictConverterImpl();

    @Spy
    private final PageParamResolver pageParamResolver =
        new PageParamResolver(Mockito.mock(SysConfigDomainService.class));

    @InjectMocks
    private SysDictTypeServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<SysDictType>> typePageCaptor;

    @Captor
    private ArgumentCaptor<SysDictType> typeCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysDictType.class, SysDictItem.class);
    }

    @Nested
    @DisplayName("pageType：分页查询字典类型")
    class PageTypeTest {

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
            given(sysDictItemMapper.selectMaps(any(QueryWrapper.class))).willReturn(
                List.of(Map.of("dict_type", "settlement_method", "cnt", 3)));

            // When
            var result = service.pageType(DictTestFactory.typeQueryDTO(null, null, null, 1, 20));

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
            var result = service.pageType(DictTestFactory.typeQueryDTO(null, null, null, 1, 20));

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
            assertThatThrownBy(() -> service.pageType(query))
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
            service.pageType(DictTestFactory.typeQueryDTO(null, null, null, 0, 20));

            // Then
            verify(sysDictTypeMapper).selectPage(typePageCaptor.capture(), any());
            assertThat(typePageCaptor.getValue().getCurrent()).isEqualTo(1L);
            assertThat(typePageCaptor.getValue().getSize()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("createType：创建字典类型")
    class CreateTypeTest {

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
            MutationVO vo = service.createType(dto);

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
            assertThatThrownBy(() -> service.createType(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_EXISTS.getCode()))
                .hasMessage("字典类型 settlement_method 已存在");
            verify(sysDictTypeMapper, never()).insert(any(SysDictType.class));
        }
    }

    @Nested
    @DisplayName("updateType：更新字典类型")
    class UpdateTypeTest {

        @Test
        @DisplayName("更新字典类型，仅更新名称，remark 为空保持原值")
        void shouldUpdateFields_whenTypeExists() {
            // Given：dict_type 不可修改，仅更新名称（状态变更走 changeStatus）
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "旧名称", 1);
            type.setRemark("原备注");
            type.setUpdateTime(LocalDateTime.of(2026, 9, 28, 12, 0, 0));
            var dto = DictTestFactory.typeUpdateDTO("新名称", null, 4);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(1);

            // When
            MutationVO vo = service.updateType(1L, dto);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 12:00:00");
            verify(sysDictTypeMapper).updateById(typeCaptor.capture());
            assertThat(typeCaptor.getValue().getDictName()).isEqualTo("新名称");
            assertThat(typeCaptor.getValue().getStatus()).isEqualTo(1);
            assertThat(typeCaptor.getValue().getRemark()).isEqualTo("原备注");
            assertThat(typeCaptor.getValue().getVersion()).isEqualTo(4);
        }

        @Test
        @DisplayName("字典类型不存在，抛出异常")
        void shouldThrow_whenTypeNotFound() {
            // Given
            given(sysDictTypeMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.updateType(1L, DictTestFactory.typeUpdateDTO("新名称", null, 1)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
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
            assertThatThrownBy(() -> service.updateType(1L, DictTestFactory.typeUpdateDTO("结算方式", null, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("deleteType：删除字典类型")
    class DeleteTypeTest {

        @Test
        @DisplayName("删除字典类型，类型下无字典项")
        void shouldDelete_whenNoItems() {
            // Given：类型下无字典项
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictItemMapper.selectCount(any())).willReturn(0L);
            given(sysDictTypeMapper.deleteById(1L)).willReturn(1);

            // When
            DeleteResultVO vo = service.deleteType(1L);

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
            assertThatThrownBy(() -> service.deleteType(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_ITEM_REFERENCED.getCode()))
                .hasMessage("字典类型 settlement_method 下存在 5 个字典项，不可删除");
            verify(sysDictTypeMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("字典类型不存在，抛出异常")
        void shouldThrow_whenTypeNotFound() {
            // Given
            given(sysDictTypeMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.deleteType(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 1 不存在");
            verifyNoInteractions(sysDictItemMapper);
        }
    }

    @Nested
    @DisplayName("changeStatusType：变更字典类型状态")
    class ChangeStatusTest {

        @Test
        @DisplayName("成功启用，仅更新状态与乐观锁版本")
        void shouldEnableType_whenStatusOne() {
            // Given：类型存在且当前停用
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 0);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(1);

            // When
            var vo = service.changeStatusType(1L, DictTestFactory.typeStatusDTO(1, 2));

            // Then：patch 实体仅承载 id/status/version
            assertThat(vo.status()).isEqualTo(1);
            verify(sysDictTypeMapper).updateById(typeCaptor.capture());
            SysDictType patch = typeCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getStatus()).isEqualTo(1);
            assertThat(patch.getVersion()).isEqualTo(2);
            assertThat(patch.getDictType()).isNull();
        }

        @Test
        @DisplayName("成功停用")
        void shouldDisableType_whenStatusZero() {
            // Given：类型存在且当前启用
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(1);

            // When
            var vo = service.changeStatusType(1L, DictTestFactory.typeStatusDTO(0, 3));

            // Then
            assertThat(vo.status()).isZero();
            verify(sysDictTypeMapper).updateById(typeCaptor.capture());
            assertThat(typeCaptor.getValue().getStatus()).isZero();
            assertThat(typeCaptor.getValue().getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("状态值非法，抛出 10001")
        void shouldThrow10001_whenStatusValueIllegal() {
            // Given
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);

            // When + Then
            assertThatThrownBy(() -> service.changeStatusType(1L, DictTestFactory.typeStatusDTO(2, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
            verify(sysDictTypeMapper, never()).updateById(any(SysDictType.class));
        }

        @Test
        @DisplayName("字典类型不存在，抛出 18028")
        void shouldThrow18028_whenTypeNotFound() {
            // Given
            given(sysDictTypeMapper.selectById(99L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.changeStatusType(99L, DictTestFactory.typeStatusDTO(1, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 99 不存在");
            verify(sysDictTypeMapper, never()).updateById(any(SysDictType.class));
        }

        @Test
        @DisplayName("乐观锁冲突（更新 0 行），抛出 10601")
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);
            given(sysDictTypeMapper.updateById(any(SysDictType.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.changeStatusType(1L, DictTestFactory.typeStatusDTO(1, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("守卫方法：requireDictTypeAvailable / requireById")
    class GuardTest {

        @Test
        @DisplayName("requireDictTypeAvailable：编码未被占用，直接放行")
        void shouldPass_whenDictTypeAvailable() {
            // Given
            given(sysDictTypeMapper.selectCount(any())).willReturn(0L);

            // When + Then（不抛异常）
            service.requireDictTypeAvailable("settlement_method");
        }

        @Test
        @DisplayName("requireDictTypeAvailable：编码已被占用，抛出 18029")
        void shouldThrow_whenDictTypeOccupied() {
            // Given
            given(sysDictTypeMapper.selectCount(any())).willReturn(2L);

            // When + Then
            assertThatThrownBy(() -> service.requireDictTypeAvailable("settlement_method"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_EXISTS.getCode()))
                .hasMessage("字典类型 settlement_method 已存在");
        }

        @Test
        @DisplayName("requireById：类型存在，返回实体")
        void shouldReturnEntity_whenTypeExists() {
            // Given
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectById(1L)).willReturn(type);

            // When
            SysDictType result = service.requireById(1L);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDictType()).isEqualTo("settlement_method");
        }

        @Test
        @DisplayName("requireById：类型不存在，抛出 18028")
        void shouldThrow_whenTypeMissing() {
            // Given
            given(sysDictTypeMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireById(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 1 不存在");
        }

        @Test
        @DisplayName("requireByDictType：类型编码存在，返回实体")
        void shouldReturnEntity_whenDictTypeExists() {
            // Given
            SysDictType type = DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1);
            given(sysDictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).willReturn(type);

            // When
            SysDictType result = service.requireByDictType("settlement_method");

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getDictType()).isEqualTo("settlement_method");
        }

        @Test
        @DisplayName("requireByDictType：类型编码不存在，抛出 18028")
        void shouldThrow_whenDictTypeMissing() {
            // Given
            given(sysDictTypeMapper.selectOne(any(LambdaQueryWrapper.class))).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireByDictType("not_exist"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 not_exist 不存在");
        }
    }

    @Nested
    @DisplayName("Entity 充血：SysDictType.applyCreateDefault")
    class EntityDefaultTest {

        @Test
        @DisplayName("status 为 null 时兜底为 1（启用）")
        void shouldDefaultStatusToEnabled_whenNull() {
            // Given
            SysDictType type = new SysDictType();

            // When
            type.applyCreateDefault();

            // Then
            assertThat(type.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("status 已指定时不覆盖")
        void shouldKeepStatus_whenPresent() {
            // Given
            SysDictType type = new SysDictType();
            type.setStatus(0);

            // When
            type.applyCreateDefault();

            // Then
            assertThat(type.getStatus()).isEqualTo(0);
        }
    }
}
