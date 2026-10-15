package me.north30.erp.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.config.mapper.SysConfigMapper;
import me.north30.erp.system.config.service.SysConfigDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 系统参数领域基础服务实现（只读取数）。
 * <p>只认启用状态（status=1）的参数；参数不存在/停用/解析失败时静默回落到消费方提供的 fallback 并 log.warn，
 * 保证配置缺失不中断业务。缓存接入后仅需替换本类内部数据源，消费方无感。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysConfigDomainServiceImpl implements SysConfigDomainService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    @Transactional(readOnly = true)
    public Long longValue(String configKey, Long fallback) {
        SysConfig config = resolve(configKey);
        if (config == null) {
            return fallback;
        }
        try {
            return Long.parseLong(config.getConfigValue().trim());
        } catch (NumberFormatException e) {
            log.warn("系统参数 {} 的值 {} 不是合法数字，使用默认值 {}", configKey, config.getConfigValue(), fallback);
            return fallback;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public String stringValue(String configKey, String fallback) {
        SysConfig config = resolve(configKey);
        return config == null ? fallback : config.getConfigValue().trim();
    }

    @Override
    @Transactional(readOnly = true)
    public Boolean booleanValue(String configKey, Boolean fallback) {
        SysConfig config = resolve(configKey);
        if (config == null) {
            return fallback;
        }
        String value = config.getConfigValue().trim();
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) {
            return Boolean.FALSE;
        }
        log.warn("系统参数 {} 的值 {} 不是合法布尔，使用默认值 {}", configKey, config.getConfigValue(), fallback);
        return fallback;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> values(Collection<String> configKeys) {
        if (configKeys == null || configKeys.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysConfig> configs = sysConfigMapper.selectList(new LambdaQueryWrapper<SysConfig>()
                .in(SysConfig::getConfigKey, configKeys)
                .eq(SysConfig::getStatus, 1));
        return configs.stream()
                .collect(Collectors.toMap(SysConfig::getConfigKey,
                        config -> config.getConfigValue().trim(), (a, b) -> a));
    }

    /**
     * 按键查询启用状态的参数（config_key 唯一索引保证至多一条，LIMIT 1 防御脏数据）。
     */
    private SysConfig resolve(String configKey) {
        return sysConfigMapper.selectOne(new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getStatus, 1)
                .last("LIMIT 1"));
    }
}
