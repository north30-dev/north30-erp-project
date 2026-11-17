package me.north30.erp.base.material.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.entity.BaseMaterialCodeMap;
import me.north30.erp.base.material.mapper.BaseMaterialCodeMapMapper;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.common.exception.BusinessException;

import org.springframework.stereotype.Component;

/**
 * 物料旧码映射策略：old_code + source_dept 唯一守卫（{@link BaseErrorCode#CODE_MAP_EXISTS} 11015）
 * 与新码必须指向已存在物料（{@link BaseErrorCode#MATERIAL_NOT_FOUND} 11002）。
 */
@Component
@RequiredArgsConstructor
public class MaterialCodeMapStrategy {

    private final BaseMaterialCodeMapMapper codeMapMapper;
    private final BaseMaterialMapper materialMapper;

    /**
     * 旧码映射唯一性守卫（old_code + source_dept 部分唯一索引的业务前置校验）。
     *
     * @param oldCode   旧编码
     * @param sourceDept 来源部门
     * @param excludeId 排除的映射 ID（修改场景传自身 ID，新增传 null）
     * @throws BusinessException 11015 旧码映射已存在
     */
    public void requireMappingAvailable(String oldCode, String sourceDept, Long excludeId) {
        LambdaQueryWrapper<BaseMaterialCodeMap> wrapper = new LambdaQueryWrapper<BaseMaterialCodeMap>()
            .eq(BaseMaterialCodeMap::getOldCode, oldCode)
            .eq(BaseMaterialCodeMap::getSourceDept, sourceDept)
            .ne(excludeId != null, BaseMaterialCodeMap::getId, excludeId);
        if (codeMapMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BaseErrorCode.CODE_MAP_EXISTS,
                "旧物料编码 " + oldCode + "（" + sourceDept + "）映射已存在");
        }
    }

    /**
     * 新码必须指向已存在物料：按 ID 加载物料，不存在抛 11002。
     *
     * @param newMaterialId 新物料 ID
     * @return 新物料实体（供回填 new_material_code）
     * @throws BusinessException 11002 新物料不存在
     */
    public BaseMaterial requireNewMaterialExists(Long newMaterialId) {
        BaseMaterial material = materialMapper.selectById(newMaterialId);
        if (material == null) {
            throw new BusinessException(BaseErrorCode.MATERIAL_NOT_FOUND, "新物料不存在（id: " + newMaterialId + "）");
        }
        return material;
    }
}
