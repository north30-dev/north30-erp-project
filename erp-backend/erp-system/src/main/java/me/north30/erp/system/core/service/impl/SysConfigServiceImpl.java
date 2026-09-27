package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.ConfigCreateDTO;
import me.north30.erp.system.core.dto.ConfigQueryDTO;
import me.north30.erp.system.core.dto.ConfigUpdateDTO;
import me.north30.erp.system.core.entity.SysConfig;
import me.north30.erp.system.core.enums.SystemManageErrorCode;
import me.north30.erp.system.core.mapper.SysConfigMapper;
import me.north30.erp.system.core.service.SysConfigService;
import me.north30.erp.system.core.util.DateTimeFormatUtil;
import me.north30.erp.system.core.vo.ConfigUpdateVO;
import me.north30.erp.system.core.vo.ConfigVO;
import me.north30.erp.system.core.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

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

    /** 值类型编码 → 名称（接口文档 5.6） */
    private static final Map<Integer, String> VALUE_TYPE_NAMES = Map.of(
        1, "字符串", 2, "数字", 3, "布尔", 4, "JSON");

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ConfigVO> page(ConfigQueryDTO query) {
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
            .like(StringUtils.hasText(query.getConfigKey()), SysConfig::getConfigKey, query.getConfigKey())
            .like(StringUtils.hasText(query.getConfigName()), SysConfig::getConfigName, query.getConfigName())
            .eq(StringUtils.hasText(query.getConfigGroup()), SysConfig::getConfigGroup, query.getConfigGroup())
            .eq(query.getStatus() != null, SysConfig::getStatus, query.getStatus())
            .orderByAsc(SysConfig::getId);
        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ConfigVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(ConfigCreateDTO dto) {
        validateValueByType(dto.valueType(), dto.configValue());
        Long exists = sysConfigMapper.selectCount(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, dto.configKey()));
        if (exists != null && exists > 0) {
            throw new BusinessException(CommonErrorCode.DUPLICATE_KEY, "参数键 " + dto.configKey() + " 已存在，请检查后重试");
        }
        SysConfig config = new SysConfig();
        config.setConfigKey(dto.configKey());
        config.setConfigName(dto.configName());
        config.setConfigValue(dto.configValue());
        config.setValueType(dto.valueType());
        config.setConfigGroup(dto.configGroup());
        config.setIsSystem(dto.isSystem() == null ? 0 : dto.isSystem());
        config.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        config.setRemark(dto.remark());
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
        validateValueByType(config.getValueType(), dto.configValue());
        config.setConfigValue(dto.configValue());
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

    /**
     * 按值类型校验参数值：2-数字须可解析为 BigDecimal；3-布尔仅允许 true/false；4-JSON 须可解析。
     */
    private void validateValueByType(Integer valueType, String value) {
        String typeName = VALUE_TYPE_NAMES.get(valueType);
        if (typeName == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "参数值类型 " + valueType + " 非法");
        }
        switch (valueType) {
            case 2 -> {
                try {
                    new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            case 3 -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            case 4 -> {
                try {
                    objectMapper.readTree(value);
                } catch (JacksonException e) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            default -> {
                // 1-字符串：不做格式校验
            }
        }
    }

    /**
     * Entity → VO 转换。
     */
    private ConfigVO toVO(SysConfig config) {
        return new ConfigVO(config.getId(), config.getConfigKey(), config.getConfigName(), config.getConfigValue(),
            config.getValueType(), config.getConfigGroup(), config.getIsSystem(), config.getStatus(),
            config.getRemark());
    }

    private long normalizePageNum(long pageNum) {
        return pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    private long normalizePageSize(long pageSize) {
        if (pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + PageConstants.MAX_PAGE_SIZE);
        }
        return pageSize;
    }
}
