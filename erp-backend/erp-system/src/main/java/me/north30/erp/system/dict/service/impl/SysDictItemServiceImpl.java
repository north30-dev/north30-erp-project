package me.north30.erp.system.dict.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.PageParamResolver;
import me.north30.erp.system.common.util.SiblingSortUtil;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.common.vo.MutationVO;
import me.north30.erp.system.dict.converter.DictConverter;
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemMoveDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemStatusDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.entity.SysDictItem;
import me.north30.erp.system.dict.mapper.SysDictItemMapper;
import me.north30.erp.system.dict.service.SysDictItemService;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictItemStatusVO;
import me.north30.erp.system.dict.vo.DictItemVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

/**
 * 字典项服务实现（A 类单层：字典项侧 4 个管理端用例 + 守卫）。
 * <p>字典项表走 {@link SysDictItemMapper}；所属类型存在性守卫单向依赖
 * {@link SysDictTypeService#requireByDictType}（item → type，type 不反向依赖 item，依赖无环）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDictItemServiceImpl implements SysDictItemService {

    /** 默认语言（E-03 预留，本期仅 zh-CN） */
    private static final String DEFAULT_LANG = "zh-CN";

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDictItemMapper sysDictItemMapper;
    private final SysDictTypeService sysDictTypeService;
    private final DictConverter dictConverter;
    private final PageParamResolver pageParamResolver;

    @Override
    @Transactional(readOnly = true)
    public PageResult<DictItemVO> pageItem(DictItemQueryDTO query) {
        if (!StringUtils.hasText(query.dictType())) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "字典类型编码不能为空");
        }
        long pageNum = pageParamResolver.normalizePageNum(query.pageNum());
        long pageSize = pageParamResolver.normalizePageSize(query.pageSize());
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
        // 所属类型存在性守卫（不存在即抛 18028）
        sysDictTypeService.requireByDictType(dto.dictType());
        String lang = StringUtils.hasText(dto.lang()) ? dto.lang() : DEFAULT_LANG;
        requireItemAvailable(dto.dictType(), dto.itemValue(), lang);
        SysDictItem item = dictConverter.createSysDictItem(dto);
        item.setLang(lang);
        item.applyCreateDefault();
        // 未显式指定显示顺序时取同分组 max(itemSort)+10（08 文档阶段四排序统一）
        if (dto.itemSort() == null) {
            item.setItemSort(nextItemSort(dto.dictType(), lang));
        }
        sysDictItemMapper.insert(item);
        return new MutationVO(item.getId(), null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MutationVO updateItem(Long id, DictItemUpdateDTO dto) {
        SysDictItem item = requireById(id);
        // null 字段跳过（部分更新语义），乐观锁 version 由下方逻辑处理
        dictConverter.updateSysDictItem(dto, item);
        item.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDictItemMapper.updateById(item));
        return new MutationVO(id, null, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void moveItem(Long id, DictItemMoveDTO dto) {
        SysDictItem item = requireById(id);
        // 同分组（dict_type + lang）按 sort ASC, id ASC 一次查出，规范化重排 + 相邻交换，变更行逐一乐观锁落库
        List<SysDictItem> siblings = sysDictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, item.getDictType())
            .eq(SysDictItem::getLang, item.getLang())
            .orderByAsc(SysDictItem::getItemSort)
            .orderByAsc(SysDictItem::getId));
        List<SysDictItem> changed = SiblingSortUtil.moveSibling(siblings, item,
            "up".equals(dto.direction()), SysDictItem::getId, SysDictItem::getItemSort, SysDictItem::setItemSort);
        for (SysDictItem row : changed) {
            UpdateGuardUtil.checkOptimisticLockConflict(sysDictItemMapper.updateById(row));
        }
    }

    /** 计算同分组（dict_type + lang）下一个显示顺序：max(itemSort) + 10，空分组返回 10。 */
    private int nextItemSort(String dictType, String lang) {
        return sysDictItemMapper.selectList(new LambdaQueryWrapper<SysDictItem>()
                .eq(SysDictItem::getDictType, dictType)
                .eq(SysDictItem::getLang, lang)).stream()
            .map(SysDictItem::getItemSort)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 10;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO deleteItem(Long id) {
        requireById(id);
        sysDictItemMapper.deleteById(id);
        return new DeleteResultVO(id, DELETED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DictItemStatusVO changeStatusItem(Long id, DictItemStatusDTO dto) {
        SysDictItem item = requireById(id);
        // 状态值域守卫（10001）收敛于实体 changeStatus，字典项无内置保护
        item.changeStatus(dto.status());
        SysDictItem patch = new SysDictItem();
        patch.setId(id);
        patch.setStatus(item.getStatus());
        patch.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDictItemMapper.updateById(patch));
        return new DictItemStatusVO(item.getStatus());
    }

    @Override
    @Transactional(readOnly = true)
    public void requireItemAvailable(String dictType, String itemValue, String lang) {
        Long exists = sysDictItemMapper.selectCount(new LambdaQueryWrapper<SysDictItem>()
            .eq(SysDictItem::getDictType, dictType)
            .eq(SysDictItem::getItemValue, itemValue)
            .eq(SysDictItem::getLang, lang));
        QueryGuardUtil.requireCountZero(exists, SystemErrorCode.DICT_ITEM_EXISTS,
            "字典项 " + itemValue + " 已存在");
    }

    @Override
    @Transactional(readOnly = true)
    public SysDictItem requireById(Long id) {
        return QueryGuardUtil.requirePresent(sysDictItemMapper.selectById(id),
            CommonErrorCode.NOT_FOUND, "字典项 " + id + " 不存在");
    }
}
