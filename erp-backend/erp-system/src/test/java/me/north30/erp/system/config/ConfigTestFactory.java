package me.north30.erp.system.config;

import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.dto.ConfigQueryDTO;
import me.north30.erp.system.config.dto.ConfigUpdateDTO;
import me.north30.erp.system.config.entity.SysConfig;

/**
 * 系统参数域测试数据静态工厂：集中构造实体与 DTO，避免测试方法内堆砌字段。
 */
public final class ConfigTestFactory {

    private ConfigTestFactory() {
    }

    /**
     * 构建系统参数实体（备注/更新时间为空，由用例按需补充）。
     */
    public static SysConfig config(Long id, String configKey, String configName, String configValue,
                                   Integer valueType, String configGroup, Integer isSystem, Integer status) {
        SysConfig config = new SysConfig();
        config.setId(id);
        config.setConfigKey(configKey);
        config.setConfigName(configName);
        config.setConfigValue(configValue);
        config.setValueType(valueType);
        config.setConfigGroup(configGroup);
        config.setIsSystem(isSystem);
        config.setStatus(status);
        return config;
    }

    /**
     * 新增系统参数请求 DTO。
     */
    public static ConfigCreateDTO createDTO(String configKey, String configName, String configValue,
                                            Integer valueType, String configGroup, Integer isSystem,
                                            Integer status, String remark) {
        return new ConfigCreateDTO(configKey, configName, configValue, valueType, configGroup,
            isSystem, status, remark);
    }

    /**
     * 修改系统参数请求 DTO。
     */
    public static ConfigUpdateDTO updateDTO(String configValue, String configName, Integer status,
                                            String remark, Integer version) {
        return new ConfigUpdateDTO(configValue, configName, status, remark, version);
    }

    /**
     * 系统参数分页查询参数。
     */
    public static ConfigQueryDTO queryDTO(String configKey, String configName, String configGroup,
                                          Integer status, Integer pageNum, Integer pageSize) {
        return new ConfigQueryDTO(configKey, configName, configGroup, status, pageNum, pageSize);
    }
}
