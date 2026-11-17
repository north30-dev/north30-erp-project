package me.north30.erp.base.materialcategory.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import lombok.RequiredArgsConstructor;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.mapper.BaseMaterialCategoryMapper;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.base.materialcategory.strategy.CategoryTreeStrategy;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.common.util.UpdateGuardUtil;

/**
 * 物料分类领域服务实现：承载本域业务规则（编码唯一、删除校验、启停状态校验、
 * 叶子标记维护、move 同级规范化重排）与本域 Mapper 网关。
 * <p>领域服务只依赖本域 Mapper 与策略类，不注入任何跨域 ApplicationService；
 * 挂载物料计数由应用服务层经物料域最小接口取得后作参数传入。</p>
 */
@Service
@RequiredArgsConstructor
public class MaterialCategoryDomainServiceImpl implements MaterialCategoryDomainService {

    /** 顶级分类父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 启用状态 */
    private static final int STATUS_ENABLED = 1;

    private final BaseMaterialCategoryMapper baseMaterialCategoryMapper;
    private final CategoryTreeStrategy categoryTreeStrategy;

    @Override
    @Transactional(readOnly = true)
    public BaseMaterialCategory requireCategory(Long id) {
        return QueryGuardUtil.requirePresent(baseMaterialCategoryMapper.selectById(id),
            CommonErrorCode.PARAM_ERROR, "物料分类 " + id + " 不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public BaseMaterialCategory requireParentCategory(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        return requireCategory(parentId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCodeAvailable(String categoryCode) {
        Long exists = baseMaterialCategoryMapper.selectCount(
            new LambdaQueryWrapper<BaseMaterialCategory>().eq(BaseMaterialCategory::getCategoryCode, categoryCode));
        if (exists != null && exists > 0) {
            throw new BusinessException(BaseErrorCode.CATEGORY_CODE_EXISTS, "分类编码 " + categoryCode + " 已存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDeletable(BaseMaterialCategory category, long mountedMaterialCount) {
        Long childCount = baseMaterialCategoryMapper.selectCount(
            new LambdaQueryWrapper<BaseMaterialCategory>().eq(BaseMaterialCategory::getParentId, category.getId()));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(BaseErrorCode.CATEGORY_HAS_CHILDREN,
                "分类 " + category.getCategoryName() + " 存在下级分类，不可删除");
        }
        // 挂载物料计数由应用服务层经物料域最小接口取得后传入
        if (mountedMaterialCount > 0) {
            throw new BusinessException(BaseErrorCode.CATEGORY_HAS_CHILDREN,
                "分类 " + category.getCategoryName() + " 已挂载物料，不可删除");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLeaf(Long categoryId) {
        return Boolean.TRUE.equals(requireCategory(categoryId).getIsLeaf());
    }

    @Override
    @Transactional(readOnly = true)
    public String requireEnabledCategoryCode(Long categoryId) {
        BaseMaterialCategory category = requireCategory(categoryId);
        if (category.getStatus() == null || category.getStatus() != STATUS_ENABLED) {
            throw new BusinessException(BaseErrorCode.MATERIAL_CATEGORY_DISABLED,
                "物料分类 " + category.getCategoryName() + " 已停用");
        }
        return category.getCategoryCode();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseMaterialCategory> listCategories(String categoryName, String categoryCode, Integer status) {
        return baseMaterialCategoryMapper.selectList(new LambdaQueryWrapper<BaseMaterialCategory>()
            .like(StringUtils.hasText(categoryName), BaseMaterialCategory::getCategoryName, categoryName)
            .like(StringUtils.hasText(categoryCode), BaseMaterialCategory::getCategoryCode, categoryCode)
            .eq(status != null, BaseMaterialCategory::getStatus, status)
            .orderByAsc(BaseMaterialCategory::getCategorySort)
            .orderByAsc(BaseMaterialCategory::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseMaterialCategory> listSiblings(Long parentId) {
        long pid = parentId == null ? ROOT_PARENT_ID : parentId;
        return baseMaterialCategoryMapper.selectList(new LambdaQueryWrapper<BaseMaterialCategory>()
            .eq(BaseMaterialCategory::getParentId, pid)
            .orderByAsc(BaseMaterialCategory::getCategorySort)
            .orderByAsc(BaseMaterialCategory::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public int nextCategorySort(Long parentId) {
        return listSiblings(parentId).stream()
            .map(BaseMaterialCategory::getCategorySort)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 10;
    }

    @Override
    public void refreshParentLeafFlag(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        BaseMaterialCategory parent = baseMaterialCategoryMapper.selectById(parentId);
        if (parent == null) {
            return;
        }
        Long childCount = baseMaterialCategoryMapper.selectCount(
            new LambdaQueryWrapper<BaseMaterialCategory>().eq(BaseMaterialCategory::getParentId, parentId));
        parent.refreshLeafFlag(childCount != null && childCount > 0);
        baseMaterialCategoryMapper.updateById(parent);
    }

    @Override
    public void insert(BaseMaterialCategory category) {
        baseMaterialCategoryMapper.insert(category);
    }

    @Override
    public void updateRow(BaseMaterialCategory category) {
        UpdateGuardUtil.checkOptimisticLockConflict(baseMaterialCategoryMapper.updateById(category));
    }

    @Override
    public void deleteById(Long id) {
        baseMaterialCategoryMapper.deleteById(id);
    }

    @Override
    public void moveCategory(Long id, boolean up) {
        BaseMaterialCategory category = requireCategory(id);
        // 同级按 sort ASC, id ASC 一次查出，规范化重排 + 相邻交换，变更行逐一乐观锁落库
        List<BaseMaterialCategory> siblings = listSiblings(category.getParentId());
        List<BaseMaterialCategory> changed = categoryTreeStrategy.moveSibling(siblings, category, up);
        for (BaseMaterialCategory row : changed) {
            updateRow(row);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> getCategoryNameMap(Set<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return Map.of();
        }
        return baseMaterialCategoryMapper.selectByIds(categoryIds).stream()
            .collect(Collectors.toMap(BaseMaterialCategory::getId, BaseMaterialCategory::getCategoryName, (a, b) -> a));
    }
}
