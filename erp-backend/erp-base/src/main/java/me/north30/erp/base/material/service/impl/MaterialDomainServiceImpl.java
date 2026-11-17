package me.north30.erp.base.material.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import lombok.RequiredArgsConstructor;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.dto.MaterialCodeMapQueryDTO;
import me.north30.erp.base.material.dto.MaterialQueryDTO;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.mapper.BaseMaterialCodeMapMapper;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.base.material.service.MaterialDomainService;
import me.north30.erp.base.material.strategy.MaterialCodeMapStrategy;
import me.north30.erp.base.material.strategy.MaterialDuplicateCheckStrategy;
import me.north30.erp.base.material.strategy.MaterialQueryStrategy;
import me.north30.erp.base.material.strategy.MaterialStatusStrategy;
import me.north30.erp.common.util.QueryGuardUtil;
import me.north30.erp.common.util.UpdateGuardUtil;

/**
 * 物料领域服务实现：承载本域业务规则（编码唯一、四要素重复、状态机流转、删除守卫、旧码映射守卫）
 * 与本域 Mapper 网关（base_material / base_material_code_map）。
 * <p>领域服务只依赖本域 Mapper 与策略类，不注入任何跨域服务；
 * 跨域数据（分类编码/叶子校验）由应用服务层编排后作参数传入。</p>
 */
@Service
@RequiredArgsConstructor
public class MaterialDomainServiceImpl implements MaterialDomainService {

    private final BaseMaterialMapper baseMaterialMapper;
    private final BaseMaterialCodeMapMapper baseMaterialCodeMapMapper;
    private final MaterialStatusStrategy materialStatusStrategy;
    private final MaterialDuplicateCheckStrategy materialDuplicateCheckStrategy;
    private final MaterialCodeMapStrategy materialCodeMapStrategy;
    private final MaterialQueryStrategy materialQueryStrategy;

    @Override
    @Transactional(readOnly = true)
    public BaseMaterial requireMaterial(Long id) {
        return QueryGuardUtil.requirePresent(baseMaterialMapper.selectById(id),
            BaseErrorCode.MATERIAL_NOT_FOUND, "物料 " + id + " 不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public BaseMaterial requireMaterialByCode(String materialCode) {
        return QueryGuardUtil.requirePresent(baseMaterialMapper.selectOne(new LambdaQueryWrapper<BaseMaterial>()
                .eq(BaseMaterial::getMaterialCode, materialCode).last("LIMIT 1")),
            BaseErrorCode.MATERIAL_NOT_FOUND, "物料 " + materialCode + " 不存在");
    }

    @Override
    @Transactional(readOnly = true)
    public void requireCodeAvailable(String materialCode, Long excludeId) {
        materialDuplicateCheckStrategy.requireCodeAvailable(materialCode, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireFourElementsAvailable(String materialName, String spec, String brand, String unit,
                                             Long excludeId) {
        materialDuplicateCheckStrategy.requireFourElementsAvailable(materialName, spec, brand, unit, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDeletable(BaseMaterial material, long referenceCount) {
        materialStatusStrategy.requireDeletable(material, referenceCount);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByCategoryId(Long categoryId) {
        Long count = baseMaterialMapper.selectCount(new LambdaQueryWrapper<BaseMaterial>()
            .eq(BaseMaterial::getCategoryId, categoryId));
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer audit(Long id, boolean approved) {
        BaseMaterial material = requireMaterial(id);
        Integer status = materialStatusStrategy.audit(material, approved);
        BaseMaterial patch = new BaseMaterial();
        patch.setId(id);
        patch.setStatus(status);
        patch.setVersion(material.getVersion());
        updateRow(patch);
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer enable(Long id) {
        BaseMaterial material = requireMaterial(id);
        Integer status = materialStatusStrategy.enable(material);
        BaseMaterial patch = new BaseMaterial();
        patch.setId(id);
        patch.setStatus(status);
        patch.setVersion(material.getVersion());
        updateRow(patch);
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer disable(Long id, String reason) {
        BaseMaterial material = requireMaterial(id);
        Integer status = materialStatusStrategy.disable(material, reason);
        BaseMaterial patch = new BaseMaterial();
        patch.setId(id);
        patch.setStatus(status);
        patch.setRemark(material.getRemark());
        patch.setVersion(material.getVersion());
        updateRow(patch);
        return status;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseMaterial insert(BaseMaterial material) {
        baseMaterialMapper.insert(material);
        return material;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRow(BaseMaterial material) {
        UpdateGuardUtil.checkOptimisticLockConflict(baseMaterialMapper.updateById(material));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        baseMaterialMapper.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseMaterial> listByIds(List<Long> ids) {
        return baseMaterialMapper.selectByIds(ids);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BaseMaterial> pageMaterials(MaterialQueryDTO query) {
        return materialQueryStrategy.pageMaterials(query);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, String> loadFourElementIndex() {
        return materialDuplicateCheckStrategy.loadFourElementIndex();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BaseMaterial> listMaterials(MaterialQueryDTO query) {
        return materialQueryStrategy.listMaterials(query);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireMappingAvailable(String oldCode, String sourceDept, Long excludeId) {
        materialCodeMapStrategy.requireMappingAvailable(oldCode, sourceDept, excludeId);
    }

    @Override
    @Transactional(readOnly = true)
    public BaseMaterial requireNewMaterialExists(Long newMaterialId) {
        return materialCodeMapStrategy.requireNewMaterialExists(newMaterialId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BaseMaterialCodeMap insertCodeMap(BaseMaterialCodeMap codeMap) {
        baseMaterialCodeMapMapper.insert(codeMap);
        return codeMap;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BaseMaterialCodeMap> pageCodeMaps(MaterialCodeMapQueryDTO query) {
        return materialQueryStrategy.pageCodeMaps(query);
    }
}
