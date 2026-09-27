package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.ConfigCreateDTO;
import me.north30.erp.system.core.dto.ConfigUpdateDTO;
import me.north30.erp.system.core.entity.SysConfig;
import me.north30.erp.system.core.mapper.SysConfigMapper;
import me.north30.erp.system.core.vo.ConfigUpdateVO;
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
 * SysConfigServiceImpl 单元测试：覆盖参数键唯一、内置参数不可删、值类型校验与乐观锁冲突。
 */
@ExtendWith(MockitoExtension.class)
class SysConfigServiceImplTest {

    @Mock
    private SysConfigMapper sysConfigMapper;

    private SysConfigServiceImpl configService;

    @BeforeEach
    void setUp() {
        configService = new SysConfigServiceImpl(sysConfigMapper, null);
    }

    private SysConfig config(Integer valueType, String value, int isSystem) {
        SysConfig config = new SysConfig();
        config.setId(1L);
        config.setConfigKey("inventory.stagnant.days");
        config.setConfigName("呆滞库存天数");
        config.setConfigValue(value);
        config.setValueType(valueType);
        config.setConfigGroup("INVENTORY");
        config.setIsSystem(isSystem);
        config.setStatus(1);
        config.setVersion(0);
        return config;
    }

    @Test
    @DisplayName("新增成功：默认非内置、启用")
    void createSuccess() {
        when(sysConfigMapper.selectCount(any())).thenReturn(0L);
        when(sysConfigMapper.insert(any(SysConfig.class))).thenAnswer(invocation -> {
            SysConfig config = invocation.getArgument(0);
            config.setId(30L);
            return 1;
        });

        MutationVO vo = configService.create(new ConfigCreateDTO(
            "inventory.stagnant.days", "呆滞库存天数", "30", 2, "INVENTORY", null, null, null));

        assertEquals(30L, vo.id());
    }

    @Test
    @DisplayName("新增失败：参数键已存在抛 10602")
    void createWithDuplicateKey() {
        when(sysConfigMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> configService.create(
            new ConfigCreateDTO("inventory.stagnant.days", "呆滞库存天数", "30", 2, "INVENTORY", null, null, null)));

        assertEquals(10602, ex.getCode());
    }

    @Test
    @DisplayName("新增失败：数字类型参数值非法抛 18034")
    void createWithInvalidNumberValue() {
        BusinessException ex = assertThrows(BusinessException.class, () -> configService.create(
            new ConfigCreateDTO("inventory.stagnant.days", "呆滞库存天数", "abc", 2, "INVENTORY", null, null, null)));

        assertEquals(18034, ex.getCode());
    }

    @Test
    @DisplayName("新增失败：布尔类型参数值非法抛 18034")
    void createWithInvalidBooleanValue() {
        BusinessException ex = assertThrows(BusinessException.class, () -> configService.create(
            new ConfigCreateDTO("inventory.flag", "开关", "yes", 3, "INVENTORY", null, null, null)));

        assertEquals(18034, ex.getCode());
    }

    @Test
    @DisplayName("修改成功：返回固定生效提示")
    void updateSuccess() {
        when(sysConfigMapper.selectById(1L)).thenReturn(config(2, "30", 0));
        when(sysConfigMapper.updateById(any(SysConfig.class))).thenReturn(1);

        ConfigUpdateVO vo = configService.update(1L, new ConfigUpdateDTO("45", null, null, null, 0));

        assertEquals("参数将在 1 分钟内生效", vo.effectNote());
    }

    @Test
    @DisplayName("修改失败：乐观锁冲突抛 10601")
    void updateWithVersionConflict() {
        when(sysConfigMapper.selectById(1L)).thenReturn(config(2, "30", 0));
        when(sysConfigMapper.updateById(any(SysConfig.class))).thenReturn(0);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> configService.update(1L, new ConfigUpdateDTO("45", null, null, null, 5)));

        assertEquals(10601, ex.getCode());
    }

    @Test
    @DisplayName("修改失败：参数不存在抛 18032")
    void updateWithMissingConfig() {
        when(sysConfigMapper.selectById(1L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> configService.update(1L, new ConfigUpdateDTO("45", null, null, null, 0)));

        assertEquals(18032, ex.getCode());
    }

    @Test
    @DisplayName("删除失败：内置参数抛 18033")
    void deleteBuiltinConfig() {
        when(sysConfigMapper.selectById(1L)).thenReturn(config(2, "30", 1));

        BusinessException ex = assertThrows(BusinessException.class, () -> configService.delete(1L));

        assertEquals(18033, ex.getCode());
        verify(sysConfigMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("删除成功：非内置参数逻辑删除")
    void deleteSuccess() {
        when(sysConfigMapper.selectById(1L)).thenReturn(config(2, "30", 0));
        when(sysConfigMapper.deleteById(1L)).thenReturn(1);

        MutationVO vo = configService.delete(1L);

        assertEquals(1, vo.isDeleted());
        verify(sysConfigMapper).deleteById(1L);
    }
}
