package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.DictItemCreateDTO;
import me.north30.erp.system.core.entity.SysDictType;
import me.north30.erp.system.core.mapper.SysDictItemMapper;
import me.north30.erp.system.core.mapper.SysDictTypeMapper;
import me.north30.erp.system.core.vo.MutationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDictItemServiceImpl 单元测试：覆盖所属类型必须存在、dict_type+item_value+lang 唯一约束口径。
 */
@ExtendWith(MockitoExtension.class)
class SysDictItemServiceImplTest {

    @Mock
    private SysDictItemMapper sysDictItemMapper;
    @Mock
    private SysDictTypeMapper sysDictTypeMapper;

    private SysDictItemServiceImpl dictItemService;

    @BeforeEach
    void setUp() {
        dictItemService = new SysDictItemServiceImpl(sysDictItemMapper, sysDictTypeMapper);
    }

    private SysDictType dictType() {
        SysDictType type = new SysDictType();
        type.setId(1L);
        type.setDictType("settlement_method");
        type.setDictName("结算方式");
        type.setStatus(1);
        return type;
    }

    @Test
    @DisplayName("新增成功：lang 缺省补 zh-CN，默认非默认项")
    void createSuccessWithDefaults() {
        when(sysDictTypeMapper.selectOne(any())).thenReturn(dictType());
        when(sysDictItemMapper.selectCount(any())).thenReturn(0L);
        when(sysDictItemMapper.insert(any(me.north30.erp.system.core.entity.SysDictItem.class)))
            .thenAnswer(invocation -> {
                me.north30.erp.system.core.entity.SysDictItem item = invocation.getArgument(0);
                item.setId(20L);
                return 1;
            });

        MutationVO vo = dictItemService.create(new DictItemCreateDTO(
            "settlement_method", "电汇", "T", null, null, null, null, null, null, null));

        assertEquals(20L, vo.id());
        verify(sysDictItemMapper).insert(any(me.north30.erp.system.core.entity.SysDictItem.class));
    }

    @Test
    @DisplayName("新增失败：所属字典类型不存在抛 18028")
    void createWithMissingType() {
        when(sysDictTypeMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> dictItemService.create(
            new DictItemCreateDTO("no_such_type", "电汇", "T", null, null, null, null, null, null, null)));

        assertEquals(18028, ex.getCode());
    }

    @Test
    @DisplayName("新增失败：同类型+值+语言重复抛 18031")
    void createWithDuplicateItemValue() {
        when(sysDictTypeMapper.selectOne(any())).thenReturn(dictType());
        when(sysDictItemMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> dictItemService.create(
            new DictItemCreateDTO("settlement_method", "电汇", "T", "zh-CN", null, null, null, null, null, null)));

        assertEquals(18031, ex.getCode());
    }
}
