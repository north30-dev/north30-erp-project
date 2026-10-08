package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.util.PageNormalizer;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.service.SysDictItemService;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.common.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典项管理服务实现。
 * <p>唯一约束口径：dict_type + item_value + lang（E-03 多语言预留）；item_value 创建后不可修改。</p>
 */
@Service
@RequiredArgsConstructor
public class SysDictItemServiceImpl implements SysDictItemService {

    /** 默认语言（E-03 预留，本期仅 zh-CN） */
    private static final String DEFAULT_LANG = "zh-CN";

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDictItemMapper sysDictItemMapper;
    private final SysDictTypeService sysDictTypeService;
    private final DictConverter dictConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictItemVO> page(DictItemQueryDTO query) {
        if (!StringUtils.hasText(query.dictType())) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        long pageNum = PageNormalizer.normalizePageNum(query.pageNum());
        long pageSize = PageNormalizer.normalizePageSize(query.pageSize());
        String lang = StringUtils.hasText(query.lang()) ? query.lang() : DEFAULT_LANG;
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, query.dictType())
            .eq(SysDictItem::getLang, lang)
            .eq(query.status() != null, SysDictItem::getStatus, query.status())
            .orderByAsc(SysDictItem::getItemSort)
            .orderByAsc(SysDictItem::getId);
        Page<SysDictItem> page = sysDictItemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DictItemVO> vos = page.getRecords().stream().map(dictConverter::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countByTypes(List<String> dictTypes) {
        if (dictTypes.isEmpty()) {
            return Map.of();
        }
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(DictItemCreateDTO dto) {
        // 校验所属类型存在（requireByDictType 不存在即抛业务异常）
        sysDictTypeService.requireByDictType(dto.dictType());
        String lang = StringUtils.hasText(dto.lang()) ? dto.lang() : DEFAULT_LANG;
        Long exists = sysDictItemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, dto.dictType())
            .eq(SysDictItem::getItemValue, dto.itemValue())
            .eq(SysDictItem::getLang, lang));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_ITEM_EXISTS, "字典项 " + dto.itemValue() + " 已存在");
        }
        SysDictItem item = dictConverter.toEntity(dto);
        item.setLang(lang);
        if (item.getItemSort() == null) {
            item.setItemSort(0);
        }
        if (item.getIsDefault() == null) {
            item.setIsDefault(0);
        }
        if (item.getStatus() == null) {
            item.setStatus(STATUS_ENABLED);
        }
        sysDictItemMapper.insert(item);
        return new MutationVO(item.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO update(Long id, DictItemUpdateDTO dto) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "字典项 " + id + " 不存在");
        }
        // null 字段跳过（部分更新语义），乐观锁 version 由下方逻辑处理
        dictConverter.updateEntity(dto, item);
        item.setVersion(dto.version());
        if (sysDictItemMapper.updateById(item) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        return new MutationVO(id, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO delete(Long id) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "字典项 " + id + " 不存在");
        }
        sysDictItemMapper.deleteById(id);
        return new MutationVO(id, null, DELETED);
    }
}
