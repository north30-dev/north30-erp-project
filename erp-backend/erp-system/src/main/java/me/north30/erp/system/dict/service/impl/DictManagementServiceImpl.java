package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.util.PageNormalizer;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.entity.SysDictType;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.mapper.SysDictTypeMapper;
import me.north30.erp.system.dict.service.DictManagementService;
import me.north30.erp.system.dict.service.SysDictItemService;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictItemVO;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.common.util.DateTimeFormatUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * 数据字典管理服务实现（字典类型 + 字典项唯一编排层）。
 * <p>跨实体守卫经基础 Service 消费：类型存在性走 {@link SysDictTypeService#requireByDictType}、
 * 字典项计数走 {@link SysDictItemService#countByTypes}；本类与两个基础 Service 均只依赖本域
 * Mapper，依赖单向为 Management → 基础 → Mapper，消除原 type/item 互调的循环依赖。</p>
 * <p>类型分页携带字典项计数：一条 group by 统计（禁 N+1）；类型删除前校验名下是否有字典项。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictManagementServiceImpl implements DictManagementService {

    /** 默认语言（E-03 预留，本期仅 zh-CN） */
    private static final String DEFAULT_LANG = "zh-CN";

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDictTypeMapper sysDictTypeMapper;
    private final SysDictItemMapper sysDictItemMapper;
    private final SysDictTypeService sysDictTypeService;
    private final SysDictItemService sysDictItemService;
    private final DictConverter dictConverter;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictTypeVO> pageType(DictTypeQueryDTO query) {
        long pageNum = PageNormalizer.normalizePageNum(query.pageNum());
        long pageSize = PageNormalizer.normalizePageSize(query.pageSize());
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
        Long exists = sysDictTypeMapper.selectCount(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dto.dictType()));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_EXISTS, "字典类型 " + dto.dictType() + " 已存在");
        }
        SysDictType dictType = dictConverter.creatSysDictItem(dto);
        if (dictType.getStatus() == null) {
            dictType.setStatus(STATUS_ENABLED);
        }
        sysDictTypeMapper.insert(dictType);
        return new MutationVO(dictType.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO updateType(Long id, DictTypeUpdateDTO dto) {
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
    public MutationVO deleteType(Long id) {
        SysDictType dictType = sysDictTypeMapper.selectById(id);
        if (dictType == null) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + id + " 不存在");
        }
        Long itemCount = sysDictItemService.countByTypes(List.of(dictType.getDictType()))
            .getOrDefault(dictType.getDictType(), 0L);
        if (itemCount > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_ITEM_REFERENCED,
                "字典类型 " + dictType.getDictType() + " 下存在 " + itemCount + " 个字典项，不可删除");
        }
        sysDictTypeMapper.deleteById(id);
        return new MutationVO(id, null, DELETED);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictItemVO> pageItem(DictItemQueryDTO query) {
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
        List<DictItemVO> vos = page.getRecords().stream()
            .map(dictConverter::toDictItemVO)
            .toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO createItem(DictItemCreateDTO dto) {
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
        SysDictItem item = dictConverter.creatSysDictItem(dto);
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
    public MutationVO updateItem(Long id, DictItemUpdateDTO dto) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "字典项 " + id + " 不存在");
        }
        // null 字段跳过（部分更新语义），乐观锁 version 由下方逻辑处理
        dictConverter.updateSysDictItem(dto, item);
        item.setVersion(dto.version());
        if (sysDictItemMapper.updateById(item) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        return new MutationVO(id, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO deleteItem(Long id) {
        SysDictItem item = sysDictItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(CommonErrorCode.NOT_FOUND, "字典项 " + id + " 不存在");
        }
        sysDictItemMapper.deleteById(id);
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
        return sysDictItemService.countByTypes(dictTypes);
    }
}
