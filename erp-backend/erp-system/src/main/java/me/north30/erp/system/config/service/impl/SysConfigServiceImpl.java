package me.north30.erp.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.config.converter.ConfigConverter;
import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.dto.ConfigQueryDTO;
import me.north30.erp.system.config.dto.ConfigUpdateDTO;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.util.PageNormalizer;
import me.north30.erp.system.config.mapper.SysConfigMapper;
import me.north30.erp.system.config.service.SysConfigService;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.system.config.vo.ConfigUpdateVO;
import me.north30.erp.system.config.vo.ConfigVO;
import me.north30.erp.system.common.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 系统参数管理服务实现。
 * <p>参数值按 valueType 校验（1-字符串 2-数字 3-布尔 4-JSON）；内置参数（is_system=1）不可删除。
 * 参数缓存（system:param:{configKey}）与事务提交后失效逻辑在缓存模块接入时补充。</p>
 */
@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    /** 内置参数标记 */
    private static final int SYSTEM_BUILTIN = 1;

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;
    private final ConfigConverter configConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ConfigVO> page(ConfigQueryDTO query) {
        long pageNum = PageNormalizer.normalizePageNum(query.pageNum());
        long pageSize = PageNormalizer.normalizePageSize(query.pageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
            .like(StringUtils.hasText(query.configKey()), SysConfig::getConfigKey, query.configKey())
            .like(StringUtils.hasText(query.configName()), SysConfig::getConfigName, query.configName())
            .eq(StringUtils.hasText(query.configGroup()), SysConfig::getConfigGroup, query.configGroup())
            .eq(query.status() != null, SysConfig::getStatus, query.status())
            .orderByAsc(SysConfig::getId);
        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ConfigVO> vos = page.getRecords().stream().map(configConverter::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(ConfigCreateDTO dto) {
        SysConfig config = configConverter.toEntity(dto);
        config.validateValue(objectMapper);
        Long exists = sysConfigMapper.selectCount(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, dto.configKey()));
        if (exists != null && exists > 0) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_KEY, "参数键 " + dto.configKey() + " 已存在，请检查后重试");
        }
        if (config.getIsSystem() == null) {
            config.setIsSystem(0);
        }
        if (config.getStatus() == null) {
            config.setStatus(STATUS_ENABLED);
        }
        sysConfigMapper.insert(config);
        return new MutationVO(config.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigUpdateVO update(Long id, ConfigUpdateDTO dto) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(SystemManageErrorCode.CONFIG_NOT_FOUND, "系统参数 " + id + " 不存在");
        }
        config.setConfigValue(dto.configValue());
        config.validateValue(objectMapper);
        if (dto.configName() != null) {
            config.setConfigName(dto.configName());
        }
        if (dto.status() != null) {
            config.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            config.setRemark(dto.remark());
        }
        config.setVersion(dto.version());
        if (sysConfigMapper.updateById(config) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        // 缓存 system:param:{configKey} 的事务提交后失效在缓存模块接入时补充
        return new ConfigUpdateVO("参数将在 1 分钟内生效", DateTimeFormatUtil.format(config.getUpdateTime()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO delete(Long id) {
        SysConfig config = sysConfigMapper.selectById(id);
        if (config == null) {
            throw new BusinessException(SystemManageErrorCode.CONFIG_NOT_FOUND, "系统参数 " + id + " 不存在");
        }
        if (config.getIsSystem() != null && config.getIsSystem() == SYSTEM_BUILTIN) {
            throw new BusinessException(SystemManageErrorCode.BUILTIN_CONFIG_UNDELETABLE,
                "内置系统参数 " + config.getConfigKey() + " 不可删除");
        }
        sysConfigMapper.deleteById(id);
        return new MutationVO(id, null, DELETED);
    }
}
