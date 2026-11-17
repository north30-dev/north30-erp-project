package me.north30.erp.base.materialcategory.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.materialcategory.converter.CategoryConverter;
import me.north30.erp.base.materialcategory.dto.CategoryCreateDTO;
import me.north30.erp.base.materialcategory.dto.CategoryMoveDTO;
import me.north30.erp.base.materialcategory.dto.CategoryStatusDTO;
import me.north30.erp.base.materialcategory.dto.CategoryTreeQueryDTO;
import me.north30.erp.base.materialcategory.dto.CategoryUpdateDTO;
import me.north30.erp.base.materialcategory.entity.BaseMaterialCategory;
import me.north30.erp.base.materialcategory.service.MaterialCategoryApplicationService;
import me.north30.erp.base.materialcategory.service.MaterialCategoryDomainService;
import me.north30.erp.base.materialcategory.strategy.CategoryTreeStrategy;
import me.north30.erp.base.materialcategory.vo.CategoryMutationVO;
import me.north30.erp.base.materialcategory.vo.CategoryStatusVO;
import me.north30.erp.base.materialcategory.vo.CategoryTreeVO;

/**
 * 物料分类应用服务实现（纯编排，零私有方法，对齐 SysUserApplicationServiceImpl 模板）。
 * <p>职责分工：状态与格式守卫进 {@link BaseMaterialCategory}（充血方法），纯规则与树构造/同级重排
 * 进 {@link MaterialCategoryDomainService} 与 {@link CategoryTreeStrategy}，DTO/VO 转换进
 * {@link CategoryConverter}（MapStruct），本类只负责"校验编排 → 转换 → 持久化 → 组装响应"。</p>
 * <p>跨域依赖仅 {@link MaterialDomainService} 最小接口（挂载物料计数，删除校验 11006 用）；
 * 不注入任何 Mapper、不写业务规则。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialCategoryApplicationServiceImpl implements MaterialCategoryApplicationService {

    private final MaterialCategoryDomainService materialCategoryDomainService;
    private final CategoryTreeStrategy categoryTreeStrategy;
    private final CategoryConverter categoryConverter;
    private final MaterialDomainService materialDomainService;

    /**
     * 物料分类树查询（一次查全量 → Converter 转 VO → 策略内存组树，禁 N+1）。
     */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryTreeVO> listTree(CategoryTreeQueryDTO query) {
        List<CategoryTreeVO> vos = materialCategoryDomainService
            .listCategories(query.categoryName(), query.categoryCode(), query.status()).stream()
            .map(categoryConverter::toTreeVO)
            .toList();
        return categoryTreeStrategy.buildTree(vos);
    }

    /**
     * 新增物料分类：编码唯一（11007）、层级上限 2 级与编码联动（实体充血）、
     * 未显式指定显示顺序取同级 max+10，新增后刷新父级叶子标记。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryMutationVO create(CategoryCreateDTO dto) {
        materialCategoryDomainService.requireCodeAvailable(dto.categoryCode());
        BaseMaterialCategory parent = materialCategoryDomainService.requireParentCategory(dto.parentId());
        BaseMaterialCategory category = categoryConverter.create(dto);
        category.applyParent(parent);
        category.applyCategoryCode(dto.categoryCode(), parent);
        if (dto.status() != null) {
            category.changeStatus(dto.status());
        }
        category.setCategorySort(dto.categorySort() != null ? dto.categorySort()
            : materialCategoryDomainService.nextCategorySort(dto.parentId()));
        category.applyCreateDefault();
        materialCategoryDomainService.insert(category);
        // 父级为顶级时 refreshParentLeafFlag 内部直接跳过
        materialCategoryDomainService.refreshParentLeafFlag(category.getParentId());
        log.info("物料分类新增完成：id={}, categoryCode={}, categoryLevel={}",
            category.getId(), category.getCategoryCode(), category.getCategoryLevel());
        return categoryConverter.toMutationVO(category);
    }

    /**
     * 修改物料分类：编码与父级不可修改（转换器无对应映射即不拷贝），选填字段 null 不覆盖，
     * 乐观锁冲突返回 409/10601。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryMutationVO update(Long id, CategoryUpdateDTO dto) {
        BaseMaterialCategory current = materialCategoryDomainService.requireCategory(id);
        BaseMaterialCategory patch = categoryConverter.update(dto);
        patch.setId(id);
        patch.setCategoryLevel(current.getCategoryLevel());
        patch.setIsLeaf(current.getIsLeaf());
        patch.setUpdateTime(LocalDateTime.now());
        materialCategoryDomainService.updateRow(patch);
        return categoryConverter.toMutationVO(patch);
    }

    /**
     * 删除物料分类（逻辑删除）：存在下级分类或已挂载物料抛 11006
     * （挂载计数在本层经物料域 {@link MaterialDomainService} 最小接口取得后传参 DomainService）。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryMutationVO delete(Long id) {
        BaseMaterialCategory category = materialCategoryDomainService.requireCategory(id);
        long mountedCount = materialDomainService.countByCategoryId(id);
        materialCategoryDomainService.requireDeletable(category, mountedCount);
        materialCategoryDomainService.deleteById(id);
        category.setIsDeleted(1);
        materialCategoryDomainService.refreshParentLeafFlag(category.getParentId());
        log.info("物料分类删除完成：id={}, categoryCode={}", id, category.getCategoryCode());
        return categoryConverter.toMutationVO(category);
    }

    /**
     * 变更分类状态（启用/停用）：状态值域守卫收敛于实体 changeStatus，乐观锁冲突返回 409/10601。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CategoryStatusVO changeStatus(Long id, CategoryStatusDTO dto) {
        BaseMaterialCategory category = materialCategoryDomainService.requireCategory(id);
        category.changeStatus(dto.status());
        BaseMaterialCategory patch = new BaseMaterialCategory();
        patch.setId(id);
        patch.setStatus(category.getStatus());
        patch.setVersion(dto.version());
        materialCategoryDomainService.updateRow(patch);
        return categoryConverter.toStatusVO(category);
    }

    /**
     * 同级上移/下移：规范化重排（10,20,30…）后相邻交换，乐观锁冲突返回 409/10601。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, CategoryMoveDTO dto) {
        materialCategoryDomainService.moveCategory(id, "up".equals(dto.direction()));
    }
}
