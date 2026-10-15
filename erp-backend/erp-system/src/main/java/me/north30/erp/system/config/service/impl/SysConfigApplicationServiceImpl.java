package me.north30.erp.system.config.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.config.converter.ConfigConverter;
import me.north30.erp.system.config.dto.ConfigCreateDTO;
import me.north30.erp.system.config.dto.ConfigQueryDTO;
import me.north30.erp.system.config.dto.ConfigUpdateDTO;
import me.north30.erp.system.config.entity.SysConfig;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.PageParamResolver;
import me.north30.erp.system.config.mapper.SysConfigMapper;
import me.north30.erp.system.config.service.SysConfigApplicationService;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.system.config.vo.ConfigUpdateVO;
import me.north30.erp.system.config.vo.ConfigVO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 系统参数管理应用服务实现（管理端用例编排）。
 * <p>参数值按 valueType 校验（1-字符串 2-数字 3-布尔 4-JSON）；内置参数（is_system=1）不可删除。
 * 跨域只读取数统一走 {@link SysConfigDomainService}，不在本层暴露。
 * 参数缓存（system:param:{configKey}）与事务提交后失效逻辑在缓存模块接入时补充。</p>
 */
@Service
@RequiredArgsConstructor
public class SysConfigApplicationServiceImpl implements SysConfigApplicationService {

    private final SysConfigMapper sysConfigMapper;
    private final ObjectMapper objectMapper;
    private final ConfigConverter configConverter;
    private final PageParamResolver pageParamResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResult<ConfigVO> page(ConfigQueryDTO dto) {
        long pageNum = pageParamResolver.normalizePageNum(dto.pageNum());
        long pageSize = pageParamResolver.normalizePageSize(dto.pageSize());
        LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<SysConfig>()
            .like(StringUtils.hasText(dto.configKey()), SysConfig::getConfigKey, dto.configKey())
            .like(StringUtils.hasText(dto.configName()), SysConfig::getConfigName, dto.configName())
            .eq(StringUtils.hasText(dto.configGroup()), SysConfig::getConfigGroup, dto.configGroup())
            .eq(dto.status() != null, SysConfig::getStatus, dto.status())
            .orderByAsc(SysConfig::getId);
        Page<SysConfig> page = sysConfigMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<ConfigVO> vos = page.getRecords().stream().map(configConverter::toConfigVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(ConfigCreateDTO dto) {
        SysConfig config = configConverter.createConfig(dto);
        config.validateValue(objectMapper);
        Long exists = sysConfigMapper.selectCount(
            new LambdaQueryWrapper<SysConfig>().eq(SysConfig::getConfigKey, dto.configKey()));
        QueryGuardUtil.requireCountZero(exists,
            CommonErrorCode.DUPLICATE_KEY, "参数键 " + dto.configKey() + " 已存在，请检查后重试");
        config.applyCreateDefault();
        sysConfigMapper.insert(config);
        return new MutationVO(config.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ConfigUpdateVO update(Long id, ConfigUpdateDTO dto) {
        SysConfig config = QueryGuardUtil.requirePresent(sysConfigMapper.selectById(id),
            SystemErrorCode.CONFIG_NOT_FOUND, "系统参数 " + id + " 不存在");
        configConverter.updateConfig(dto, config);
        config.validateValue(objectMapper);
        config.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysConfigMapper.updateById(config));
        // 缓存 system:param:{configKey} 的事务提交后失效在缓存模块接入时补充
        return new ConfigUpdateVO("参数将在 1 分钟内生效", DateTimeFormatUtil.format(config.getUpdateTime()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysConfig config = QueryGuardUtil.requirePresent(sysConfigMapper.selectById(id),
            SystemErrorCode.CONFIG_NOT_FOUND, "系统参数 " + id + " 不存在");
        config.validateIsSystem();
        sysConfigMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
    }
}
