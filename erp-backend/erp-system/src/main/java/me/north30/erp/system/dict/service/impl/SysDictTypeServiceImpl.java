package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.common.util.DateTimeFormatUtil;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.system.common.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字典类型管理服务实现。
 * <p>分页携带字典项计数：一条 group by 统计（禁 N+1）；删除前校验类型下是否有字典项。</p>
 */
@Service
@RequiredArgsConstructor
public class SysDictTypeServiceImpl implements SysDictTypeService {

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;
    private final DictConverter dictConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictTypeVO> page(DictTypeQueryDTO query) {
        long pageNum = normalizePageNum(query.pageNum());
        long pageSize = normalizePageSize(query.pageSize());
        LambdaQueryWrapper<SysDictType> wrapper = new LambdaQueryWrapper<SysDictType>()
            .like(StringUtils.hasText(query.dictType()), SysDictType::getDictType, query.dictType())
            .like(StringUtils.hasText(query.dictName()), SysDictType::getDictName, query.dictName())
            .eq(query.status() != null, SysDictType::getStatus, query.status())
            .orderByAsc(SysDictType::getId);
        Page<SysDictType> page = sysDictTypeMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<String, Long> itemCounts = countItemsByTypes(page.getRecords());
        List<DictTypeVO> vos = page.getRecords().stream()
            .map(type -> dictConverter.toVO(type, itemCounts.getOrDefault(type.getDictType(), 0L)))
            .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(DictTypeCreateDTO dto) {
        Long exists = sysDictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dto.dictType()));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_EXISTS, "字典类型 " + dto.dictType() + " 已存在");
        }
        SysDictType dictType = dictConverter.toEntity(dto);
        if (dictType.getStatus() == null) {
            dictType.setStatus(STATUS_ENABLED);
        }
        sysDictTypeMapper.insert(dictType);
        return new MutationVO(dictType.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO update(Long id, DictTypeUpdateDTO dto) {
        SysDictType dictType = sysDictTypeMapper.selectById(id);
        if (dictType == null) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + id + " 不存在");
        }
        dictType.setDictName(dto.dictName());
        if (dto.status() != null) {
            dictType.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            dictType.setRemark(dto.remark());
        }
        dictType.setVersion(dto.version());
        if (sysDictTypeMapper.updateById(dictType) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        return new MutationVO(id, DateTimeFormatUtil.format(dictType.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO delete(Long id) {
        SysDictType dictType = sysDictTypeMapper.selectById(id);
        if (dictType == null) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + id + " 不存在");
        }
        Long itemCount = sysDictItemMapper.selectCount(
            new LambdaQueryWrapper<SysDictItem>().eq(SysDictItem::getDictType, dictType.getDictType()));
        if (itemCount != null && itemCount > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_ITEM_REFERENCED,
                "字典类型 " + dictType.getDictType() + " 下存在 " + itemCount + " 个字典项，不可删除");
        }
        sysDictTypeMapper.deleteById(id);
        return new MutationVO(id, null, DELETED);
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

    private long normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum <= 0 ? PageConstants.DEFAULT_PAGE_NUM : pageNum;
    }

    private long normalizePageSize(Integer pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        if (pageSize > PageConstants.MAX_PAGE_SIZE) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                "pageSize 不能超过 " + PageConstants.MAX_PAGE_SIZE);
        }
        return pageSize;
    }
}
