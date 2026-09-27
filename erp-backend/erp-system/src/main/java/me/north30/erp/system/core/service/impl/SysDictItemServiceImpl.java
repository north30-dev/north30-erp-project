package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.core.dto.DictItemCreateDTO;
import me.north30.erp.system.core.dto.DictItemQueryDTO;
import me.north30.erp.system.core.dto.DictItemUpdateDTO;
import me.north30.erp.system.core.entity.SysDictItem;
import me.north30.erp.system.core.entity.SysDictType;
import me.north30.erp.system.core.enums.SystemManageErrorCode;
import me.north30.erp.system.core.mapper.SysDictItemMapper;
import me.north30.erp.system.core.mapper.SysDictTypeMapper;
import me.north30.erp.system.core.service.SysDictItemService;
import me.north30.erp.system.core.vo.DictItemVO;
import me.north30.erp.system.core.vo.MutationVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

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
    private final SysDictTypeMapper sysDictTypeMapper;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictItemVO> page(DictItemQueryDTO query) {
        if (!StringUtils.hasText(query.getDictType())) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        long pageNum = normalizePageNum(query.getPageNum());
        long pageSize = normalizePageSize(query.getPageSize());
        String lang = StringUtils.hasText(query.getLang()) ? query.getLang() : DEFAULT_LANG;
        LambdaQueryWrapper<SysDictItem> wrapper = new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, query.getDictType())
            .eq(SysDictItem::getLang, lang)
            .eq(query.getStatus() != null, SysDictItem::getStatus, query.getStatus())
            .orderByAsc(SysDictItem::getItemSort)
            .orderByAsc(SysDictItem::getId);
        Page<SysDictItem> page = sysDictItemMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<DictItemVO> vos = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(page.getTotal(), pageNum, pageSize, vos);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO create(DictItemCreateDTO dto) {
        SysDictType dictType = sysDictTypeMapper.selectOne(
            new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getDictType, dto.dictType()));
        if (dictType == null) {
            throw new BusinessException(SystemManageErrorCode.DICT_TYPE_NOT_FOUND, "字典类型 " + dto.dictType() + " 不存在");
        }
        String lang = StringUtils.hasText(dto.lang()) ? dto.lang() : DEFAULT_LANG;
        Long exists = sysDictItemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, dto.dictType())
            .eq(SysDictItem::getItemValue, dto.itemValue())
            .eq(SysDictItem::getLang, lang));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DICT_ITEM_EXISTS, "字典项 " + dto.itemValue() + " 已存在");
        }
        SysDictItem item = new SysDictItem();
        item.setDictType(dto.dictType());
        item.setItemLabel(dto.itemLabel());
        item.setItemValue(dto.itemValue());
        item.setLang(lang);
        item.setItemSort(dto.itemSort() == null ? 0 : dto.itemSort());
        item.setCssClass(dto.cssClass());
        item.setIsDefault(dto.isDefault() == null ? 0 : dto.isDefault());
        item.setExtJson(dto.extJson());
        item.setStatus(dto.status() == null ? STATUS_ENABLED : dto.status());
        item.setRemark(dto.remark());
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
        if (dto.itemLabel() != null) {
            item.setItemLabel(dto.itemLabel());
        }
        if (dto.itemSort() != null) {
            item.setItemSort(dto.itemSort());
        }
        if (dto.cssClass() != null) {
            item.setCssClass(dto.cssClass());
        }
        if (dto.isDefault() != null) {
            item.setIsDefault(dto.isDefault());
        }
        if (dto.extJson() != null) {
            item.setExtJson(dto.extJson());
        }
        if (dto.status() != null) {
            item.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            item.setRemark(dto.remark());
        }
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

    /**
     * Entity → VO 转换（字典缓存后续阶段接入，cached 恒为 false）。
     */
    private DictItemVO toVO(SysDictItem item) {
        return new DictItemVO(item.getId(), item.getDictType(), item.getItemLabel(), item.getItemValue(),
            item.getLang(), item.getItemSort(), item.getCssClass(), item.getIsDefault(),
            item.getExtJson(), item.getStatus(), item.getRemark(), Boolean.FALSE);
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
