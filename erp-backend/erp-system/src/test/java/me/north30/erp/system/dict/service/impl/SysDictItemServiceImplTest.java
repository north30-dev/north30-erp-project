package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.DictTestFactory;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.converter.DictConverterImpl;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.service.SysDictItemService;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.support.MpTableInfoInit;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysDictItemServiceImpl} 纯单元测试：字典项侧 4 个管理端用例
 * （分页语言过滤、类型存在性、唯一性、乐观锁）与守卫方法（requireItemAvailable / requireById）。
 */
@ExtendWith(MockitoExtension.class)
class SysDictItemServiceImplTest {

    @Mock
    private SysDictItemMapper sysDictItemMapper;

    @Mock
    private SysDictTypeService sysDictTypeService;

    @Spy
    private final DictConverter dictConverter = new DictConverterImpl();

    @Spy
    private final PageParamResolver pageParamResolver =
        new PageParamResolver(Mockito.mock(SysConfigDomainService.class));

    @InjectMocks
    private SysDictItemServiceImpl service;

    @Captor
    private ArgumentCaptor<Page<SysDictItem>> itemPageCaptor;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<SysDictItem>> itemWrapperCaptor;

    @Captor
    private ArgumentCaptor<SysDictItem> itemCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysDictItem.class);
    }

    @Nested
    @DisplayName("pageItem：分页查询字典项")
    class PageItemTest {

        @Test
        @DisplayName("字典类型编码为空，抛出异常")
        void shouldThrow_whenDictTypeBlank() {
            // Given：字典类型编码为空串
            var query = DictTestFactory.itemQueryDTO("", null, null, 1, 20);

            // When + Then
            assertThatThrownBy(() -> service.pageItem(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("字典类型编码不能为空");
            verifyNoInteractions(sysDictItemMapper, sysDictTypeService);
        }

        @Test
        @DisplayName("查询有效参数，返回分页结果")
        void shouldReturnItems_whenQueryValid() {
            // Given
            SysDictItem item = DictTestFactory.dictItem(1L, "settlement_method", "现金", "CASH", "zh-CN", 1);
            Page<SysDictItem> page = new Page<>(1, 20);
            page.setTotal(1);
            page.setRecords(List.of(item));
            given(sysDictItemMapper.selectPage(any(), any())).willReturn(page);

            // When
            var result = service.pageItem(DictTestFactory.itemQueryDTO("settlement_method", null, 1, 1, 20));

            // Then：VO 字段完整映射，cached 恒为 false
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.pages()).isEqualTo(1L);
            List<DictItemVO> vos = result.list();
            assertThat(vos).hasSize(1);
            DictItemVO vo = vos.get(0);
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.dictType()).isEqualTo("settlement_method");
            assertThat(vo.itemLabel()).isEqualTo("现金");
            assertThat(vo.itemValue()).isEqualTo("CASH");
            assertThat(vo.lang()).isEqualTo("zh-CN");
            assertThat(vo.itemSort()).isEqualTo(1);
            assertThat(vo.isDefault()).isZero();
            assertThat(vo.status()).isEqualTo(1);
            assertThat(vo.cached()).isFalse();
        }

        @Test
        @DisplayName("未指定语言时按 zh-CN 过滤")
        void shouldDefaultLangToZhCN_whenLangMissing() {
            // Given：未指定语言时按 zh-CN 过滤
            Page<SysDictItem> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysDictItemMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.pageItem(DictTestFactory.itemQueryDTO("settlement_method", null, null, 1, 20));

            // Then：默认语言条件已写入查询参数（MP 条件参数在渲染 SQL 片段时才落参，需先触发渲染）
            verify(sysDictItemMapper).selectPage(any(), itemWrapperCaptor.capture());
            var wrapper = itemWrapperCaptor.getValue();
            wrapper.getSqlSegment();
            assertThat(wrapper.getParamNameValuePairs()).containsValue("zh-CN");
        }

        @Test
        @DisplayName("显式传入语言编码，按语言过滤")
        void shouldUseProvidedLang_whenLangPresent() {
            // Given：显式传入语言编码
            Page<SysDictItem> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysDictItemMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.pageItem(DictTestFactory.itemQueryDTO("settlement_method", "en-US", null, 1, 20));

            // Then：显式语言条件已写入查询参数（渲染 SQL 片段触发落参）
            verify(sysDictItemMapper).selectPage(any(), itemWrapperCaptor.capture());
            var wrapper = itemWrapperCaptor.getValue();
            wrapper.getSqlSegment();
            var params = wrapper.getParamNameValuePairs().values();
            assertThat(params).contains("en-US");
            assertThat(params).doesNotContain("zh-CN");
        }

        @Test
        @DisplayName("分页大小超过 200，抛出异常")
        void shouldThrow_whenPageSizeExceedsLimit() {
            // Given
            var query = DictTestFactory.itemQueryDTO("settlement_method", null, null, 1, 201);

            // When + Then
            assertThatThrownBy(() -> service.pageItem(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("pageSize 不能超过 200");
            verifyNoInteractions(sysDictItemMapper, sysDictTypeService);
        }

        @Test
        @DisplayName("页码为 0 时归位为 1")
        void shouldNormalizePageNum_whenNonPositive() {
            // Given：页码为 0 时归位为 1
            Page<SysDictItem> page = new Page<>(1, 20);
            page.setTotal(0);
            page.setRecords(List.of());
            given(sysDictItemMapper.selectPage(any(), any())).willReturn(page);

            // When
            service.pageItem(DictTestFactory.itemQueryDTO("settlement_method", null, null, 0, 20));

            // Then
            verify(sysDictItemMapper).selectPage(itemPageCaptor.capture(), any());
            assertThat(itemPageCaptor.getValue().getCurrent()).isEqualTo(1L);
            assertThat(itemPageCaptor.getValue().getSize()).isEqualTo(20L);
        }
    }

    @Nested
    @DisplayName("createItem：创建字典项")
    class CreateItemTest {

        @Test
        @DisplayName("类型存在，lang/isDefault/itemSort/status 为空触发默认值")
        void shouldInsertWithDefaults_whenLangMissing() {
            // Given：类型存在，lang/isDefault/itemSort/status 为空触发默认值
            var dto = DictTestFactory.itemCreateDTO("settlement_method", "现金", "CASH", null, null, null);
            given(sysDictTypeService.requireByDictType("settlement_method")).willReturn(
                DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1));
            given(sysDictItemMapper.selectCount(any())).willReturn(0L);
            given(sysDictItemMapper.insert(any(SysDictItem.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysDictItem.class).setId(9L);
                return 1;
            });

            // When
            MutationVO vo = service.createItem(dto);

            // Then：唯一键口径 dict_type + item_value + lang(zh-CN)
            assertThat(vo.id()).isEqualTo(9L);
            verify(sysDictItemMapper).insert(itemCaptor.capture());
            SysDictItem inserted = itemCaptor.getValue();
            assertThat(inserted.getDictType()).isEqualTo("settlement_method");
            assertThat(inserted.getItemLabel()).isEqualTo("现金");
            assertThat(inserted.getItemValue()).isEqualTo("CASH");
            assertThat(inserted.getLang()).isEqualTo("zh-CN");
            assertThat(inserted.getItemSort()).isZero();
            assertThat(inserted.getIsDefault()).isZero();
            assertThat(inserted.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("所属字典类型不存在，抛出异常")
        void shouldThrow_whenDictTypeMissing() {
            // Given：类型守卫抛出"类型不存在"业务异常（异常语义由类型守卫自身实现与测试保证）
            var dto = DictTestFactory.itemCreateDTO("settlement_method", "现金", "CASH", null, 1, 1);
            given(sysDictTypeService.requireByDictType(any()))
                .willThrow(new BusinessException(SystemErrorCode.DICT_TYPE_NOT_FOUND,
                    "字典类型 settlement_method 不存在"));

            // When + Then
            assertThatThrownBy(() -> service.createItem(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_TYPE_NOT_FOUND.getCode()))
                .hasMessage("字典类型 settlement_method 不存在");
            verify(sysDictItemMapper, never()).insert(any(SysDictItem.class));
        }

        @Test
        @DisplayName("同 dict_type + item_value + lang 已存在，抛出异常")
        void shouldThrow_whenItemValueDuplicated() {
            // Given：同 dict_type + item_value + lang 已存在
            var dto = DictTestFactory.itemCreateDTO("settlement_method", "现金", "CASH", null, 1, 1);
            given(sysDictTypeService.requireByDictType("settlement_method")).willReturn(
                DictTestFactory.dictType(1L, "settlement_method", "结算方式", 1));
            given(sysDictItemMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.createItem(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_ITEM_EXISTS.getCode()))
                .hasMessage("字典项 CASH 已存在");
            verify(sysDictItemMapper, never()).insert(any(SysDictItem.class));
        }
    }

    @Nested
    @DisplayName("updateItem：更新字典项")
    class UpdateItemTest {

        @Test
        @DisplayName("更新字典项，仅应用非空字段，itemValue 保持不变")
        void shouldUpdateFields_whenItemExists() {
            // Given：item_value 与 lang 不在修改范围
            SysDictItem item = DictTestFactory.dictItem(1L, "settlement_method", "现金", "CASH", "zh-CN", 5);
            var dto = DictTestFactory.itemUpdateDTO(2, "现金（更新）", 1, 0);
            given(sysDictItemMapper.selectById(1L)).willReturn(item);
            given(sysDictItemMapper.updateById(any(SysDictItem.class))).willReturn(1);

            // When
            MutationVO vo = service.updateItem(1L, dto);

            // Then：仅应用非空字段，itemValue 保持不变
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isNull();
            verify(sysDictItemMapper).updateById(itemCaptor.capture());
            SysDictItem updated = itemCaptor.getValue();
            assertThat(updated.getItemLabel()).isEqualTo("现金（更新）");
            assertThat(updated.getItemSort()).isEqualTo(1);
            assertThat(updated.getStatus()).isEqualTo(0);
            assertThat(updated.getItemValue()).isEqualTo("CASH");
            assertThat(updated.getVersion()).isEqualTo(2);
        }

        @Test
        @DisplayName("字典项不存在，抛出异常")
        void shouldThrow_whenItemNotFound() {
            // Given
            given(sysDictItemMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.updateItem(1L, DictTestFactory.itemUpdateDTO(1, "现金", null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode()))
                .hasMessage("字典项 1 不存在");
            verify(sysDictItemMapper, never()).updateById(any(SysDictItem.class));
        }

        @Test
        @DisplayName("数据已被其他操作修改，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given
            SysDictItem item = DictTestFactory.dictItem(1L, "settlement_method", "现金", "CASH", "zh-CN", 1);
            given(sysDictItemMapper.selectById(1L)).willReturn(item);
            given(sysDictItemMapper.updateById(any(SysDictItem.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.updateItem(1L, DictTestFactory.itemUpdateDTO(9, "现金", null, null)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("deleteItem：删除字典项")
    class DeleteItemTest {

        @Test
        @DisplayName("删除字典项")
        void shouldDelete_whenItemExists() {
            // Given
            SysDictItem item = DictTestFactory.dictItem(1L, "settlement_method", "现金", "CASH", "zh-CN", 1);
            given(sysDictItemMapper.selectById(1L)).willReturn(item);
            given(sysDictItemMapper.deleteById(1L)).willReturn(1);

            // When
            DeleteResultVO vo = service.deleteItem(1L);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysDictItemMapper).deleteById(1L);
            verifyNoInteractions(sysDictTypeService);
        }

        @Test
        @DisplayName("字典项不存在，抛出异常")
        void shouldThrow_whenItemNotFound() {
            // Given
            given(sysDictItemMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.deleteItem(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode()))
                .hasMessage("字典项 1 不存在");
            verify(sysDictItemMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("守卫方法：requireItemAvailable / requireById")
    class GuardTest {

        @Test
        @DisplayName("requireItemAvailable：唯一键未被占用，直接放行")
        void shouldPass_whenItemAvailable() {
            // Given
            given(sysDictItemMapper.selectCount(any())).willReturn(0L);

            // When + Then（不抛异常）
            service.requireItemAvailable("settlement_method", "CASH", "zh-CN");
        }

        @Test
        @DisplayName("requireItemAvailable：唯一键已被占用，抛出 18031")
        void shouldThrow_whenItemOccupied() {
            // Given
            given(sysDictItemMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.requireItemAvailable("settlement_method", "CASH", "zh-CN"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DICT_ITEM_EXISTS.getCode()))
                .hasMessage("字典项 CASH 已存在");
        }

        @Test
        @DisplayName("requireById：字典项存在，返回实体")
        void shouldReturnEntity_whenItemExists() {
            // Given
            SysDictItem item = DictTestFactory.dictItem(1L, "settlement_method", "现金", "CASH", "zh-CN", 1);
            given(sysDictItemMapper.selectById(1L)).willReturn(item);

            // When
            SysDictItem result = service.requireById(1L);

            // Then
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getItemValue()).isEqualTo("CASH");
        }

        @Test
        @DisplayName("requireById：字典项不存在，抛出 10004")
        void shouldThrow_whenItemMissing() {
            // Given
            given(sysDictItemMapper.selectById(1L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireById(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.NOT_FOUND.getCode()))
                .hasMessage("字典项 1 不存在");
        }
    }
}
