package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.PageParamResolver;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictTypeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典类型服务实现（A 类单层：类型侧 4 个管理端用例 + 守卫）。
 * <p>只依赖本域两个 Mapper：类型表走 {@link SysDictTypeMapper}，字典项计数走
 * {@link SysDictItemMapper}（一条 group by 统计，禁 N+1），不依赖字典项 Service，依赖无环。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;
    private final DictConverter dictConverter;
    private final PageParamResolver pageParamResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictTypeVO> pageType(DictTypeQueryDTO query) {
        long pageNum = pageParamResolver.normalizePageNum(query.pageNum());
        long pageSize = pageParamResolver.normalizePageSize(query.pageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
            .like(StringUtils.hasText(query.dictType()), SysDictType::getDictType, query.dictType())
            .like(StringUtils.hasText(query.dictName()), SysDictType::getDictName, query.dictName())
            .eq(query.status() != null, SysDictType::getStatus, query.status())
            .orderByAsc(SysDictType::getId);
        Page<SysDictType> page = sysDictTypeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Long> itemCounts = countItemsByTypes(page.getRecords());
        List<DictTypeVO> vos = page.getRecords().stream()
            .map(type -> dictConverter.toDictTypeVO(type, itemCounts.getOrDefault(type.getDictType(), 0L)))
            .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO createType(DictTypeCreateDTO dto) {
        requireDictTypeAvailable(dto.dictType());
        SysDictType dictType = dictConverter.createSysDictType(dto);
        dictType.applyCreateDefault();
        sysDictTypeMapper.insert(dictType);
        return new MutationVO(dictType.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO updateType(Long id, DictTypeUpdateDTO dto) {
        SysDictType dictType = requireById(id);
        dictConverter.updateSysDictType(dto, dictType);
        dictType.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDictTypeMapper.updateById(dictType));
        return new MutationVO(id, DateTimeFormatUtil.format(dictType.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO deleteType(Long id) {
        SysDictType dictType = requireById(id);
        Long itemCount = sysDictItemMapper.selectCount(
            new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictType, dictType.getDictType()));
        QueryGuardUtil.requireCountZero(itemCount, SystemErrorCode.DICT_ITEM_REFERENCED,
            "字典类型 " + dictType.getDictType() + " 下存在 " + itemCount + " 个字典项，不可删除");
        sysDictTypeMapper.deleteById(id);
        return new DeleteResultVO(id, DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDictTypeAvailable(String dictType) {
        Long exists = sysDictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType));
        QueryGuardUtil.requireCountZero(exists, SystemErrorCode.DICT_TYPE_EXISTS,
            "字典类型 " + dictType + " 已存在");
    }

    @Override
    @Transactional(readOnly = true)
    public SysDictType requireById(Long id) {
        return QueryGuardUtil.requirePresent(sysDictTypeMapper.selectById(id),
            SystemErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + id + " 不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public SysDictType requireByDictType(String dictType) {
        return QueryGuardUtil.requirePresent(sysDictTypeMapper.selectOne(
                new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dictType)),
            SystemErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + dictType + " 不存在");
    }

    /**
     * 按类型编码批量统计字典项数量（一条 group by 查询，禁 N+1）。
     */
    private Map<String, Long> countItemsByTypes(List<SysDictType> types) {
        if (types.isEmpty()) {
            return Map.of();
        }
        List<String> dictTypes = types.stream().map(SysDictType::getDictType).toList();
        QueryWrapper<SysDictItem> countWrapper = new QueryWrapper<SysDictItem>()
            .select("dict_type", "count(1) AS cnt")
            .in("dict_type", dictTypes)
            .groupBy("dict_type");
        List<Map<String, Object>> rows = sysDictItemMapper.selectMaps(countWrapper);
        Map<String, Long> counts = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object dictType = row.get("dict_type");
            Object cnt = row.get("cnt");
            if (dictType != null && cnt != null) {
                counts.put(dictType.toString(), ((Number) cnt).longValue());
            }
        }
        return counts;
    }
}
