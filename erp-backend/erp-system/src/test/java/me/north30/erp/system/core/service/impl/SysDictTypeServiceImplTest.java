package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.DictTypeCreateDTO;
import me.north30.erp.system.core.dto.DictTypeUpdateDTO;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDictTypeServiceImpl 单元测试：覆盖类型编码唯一、类型不存在与"类型下有字典项不可删"。
 */
@ExtendWith(MockitoExtension.class)
class SysDictTypeServiceImplTest {

    @Mock
    private SysDictTypeMapper sysDictTypeMapper;
    @Mock
    private SysDictItemMapper sysDictItemMapper;

    private SysDictTypeServiceImpl dictTypeService;

    @BeforeEach
    void setUp() {
        dictTypeService = new SysDictTypeServiceImpl(sysDictTypeMapper, sysDictItemMapper);
    }

    private SysDictType dictType() {
        SysDictType type = new SysDictType();
        type.setId(1L);
        type.setDictType("settlement_method");
        type.setDictName("结算方式");
        type.setStatus(1);
        type.setVersion(0);
        return type;
    }

    @Test
    @DisplayName("新增成功：默认启用")
    void createSuccess() {
        when(sysDictTypeMapper.selectCount(any())).thenReturn(0L);
        when(sysDictTypeMapper.insert(any(SysDictType.class))).thenAnswer(invocation -> {
            SysDictType type = invocation.getArgument(0);
            type.setId(10L);
            return 1;
        });

        MutationVO vo = dictTypeService.create(new DictTypeCreateDTO("settlement_method", "结算方式", null, null));

        assertEquals(10L, vo.id());
    }

    @Test
    @DisplayName("新增失败：类型编码已存在抛 18029")
    void createWithDuplicateType() {
        when(sysDictTypeMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> dictTypeService.create(new DictTypeCreateDTO("settlement_method", "结算方式", null, null)));

        assertEquals(18029, ex.getCode());
    }

    @Test
    @DisplayName("修改失败：类型不存在抛 18028")
    void updateWithMissingType() {
        when(sysDictTypeMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> dictTypeService.update(1L, new DictTypeUpdateDTO("结算方式", 1, null, 0)));

        assertEquals(18028, ex.getCode());
    }

    @Test
    @DisplayName("修改成功：乐观锁冲突抛 10601")
    void updateWithVersionConflict() {
        when(sysDictTypeMapper.selectById(1L)).thenReturn(dictType());
        when(sysDictTypeMapper.updateById(any(SysDictType.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> dictTypeService.update(1L, new DictTypeUpdateDTO("结算方式", 1, null, 5)));

        assertEquals(10601, ex.getCode());
    }

    @Test
    @DisplayName("删除失败：类型下存在字典项抛 18030")
    void deleteWithItems() {
        when(sysDictTypeMapper.selectById(1L)).thenReturn(dictType());
        when(sysDictItemMapper.selectCount(any())).thenReturn(4L);

        BusinessException ex = assertThrows(BusinessException.class, () -> dictTypeService.delete(1L));

        assertEquals(18030, ex.getCode());
        verify(sysDictTypeMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("删除成功：无字典项时逻辑删除类型")
    void deleteSuccess() {
        when(sysDictTypeMapper.selectById(1L)).thenReturn(dictType());
        when(sysDictItemMapper.selectCount(any())).thenReturn(0L);
        when(sysDictTypeMapper.deleteById(1L)).thenReturn(1);

        MutationVO vo = dictTypeService.delete(1L);

        assertEquals(1, vo.isDeleted());
        verify(sysDictTypeMapper).deleteById(1L);
    }
}
