package me.north30.erp.base.material.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.base.common.enums.BaseErrorCode;
import me.north30.erp.base.material.entity.BaseMaterial;
import me.north30.erp.base.material.mapper.BaseMaterialMapper;
import me.north30.erp.common.exception.BusinessException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 物料重复检测策略：编码唯一性（{@link BaseErrorCode#MATERIAL_CODE_EXISTS} 11001）与
 * 四要素重复提示（名称 + 规格 + 品牌 + 单位 → {@link BaseErrorCode#MATERIAL_DUPLICATED} 11008）。
 * <p>四要素规格/品牌允许为空，查重条件按"传值 eq / 空值 isNull"构造；
 * 批量导入场景经 {@link #loadFourElementIndex()} 一次预载既有四要素索引后内存查重，避免行级 N+1。</p>
 */
@Component
@RequiredArgsConstructor
public class MaterialDuplicateCheckStrategy {

    private final BaseMaterialMapper materialMapper;

    /**
     * 编码唯一性守卫（接口文档 2.3：11001 物料编码已存在；新增/导入自定义编码场景）。
     *
     * @param materialCode 物料编码
     * @param excludeId    排除的物料 ID（修改场景传自身 ID，新增传 null）
     * @throws BusinessException 11001 编码已存在
     */
    public void requireCodeAvailable(String materialCode, Long excludeId) {
        LambdaQueryWrapper<BaseMaterial> wrapper = new LambdaQueryWrapper<BaseMaterial>()
            .eq(BaseMaterial::getMaterialCode, materialCode)
            .ne(excludeId != null, BaseMaterial::getId, excludeId);
        if (materialMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(BaseErrorCode.MATERIAL_CODE_EXISTS, "物料编码 " + materialCode + " 已存在");
        }
    }

    /**
     * 四要素重复守卫（11008：名称 + 规格 + 品牌 + 单位与既有物料一致，仅提示不阻断数据合理性）。
     *
     * @param materialName 物料名称
     * @param spec         规格型号（可空，空值按 isNull 匹配）
     * @param brand        品牌（可空，空值按 isNull 匹配）
     * @param unit         计量单位
     * @param excludeId    排除的物料 ID（修改场景传自身 ID，新增传 null）
     * @throws BusinessException 11008 已存在相同物料
     */
    public void requireFourElementsAvailable(String materialName, String spec, String brand, String unit,
                                             Long excludeId) {
        LambdaQueryWrapper<BaseMaterial> wrapper = new LambdaQueryWrapper<BaseMaterial>()
            .eq(BaseMaterial::getMaterialName, materialName)
            .eq(StringUtils.hasText(spec), BaseMaterial::getSpec, spec)
            .isNull(!StringUtils.hasText(spec), BaseMaterial::getSpec)
            .eq(StringUtils.hasText(brand), BaseMaterial::getBrand, brand)
            .isNull(!StringUtils.hasText(brand), BaseMaterial::getBrand)
            .eq(BaseMaterial::getUnit, unit)
            .ne(excludeId != null, BaseMaterial::getId, excludeId)
            .last("LIMIT 1");
        BaseMaterial duplicated = materialMapper.selectOne(wrapper);
        if (duplicated != null) {
            throw new BusinessException(BaseErrorCode.MATERIAL_DUPLICATED,
                "已存在相同物料（" + duplicated.getMaterialCode() + "），请确认是否重复");
        }
    }

    /**
     * 预载既有物料四要素索引（导入场景单次查询，键 → 既有物料编码，供行级内存查重）。
     *
     * @return 四要素规范化键 → 既有物料编码
     */
    public Map<String, String> loadFourElementIndex() {
        List<BaseMaterial> rows = materialMapper.selectList(new LambdaQueryWrapper<BaseMaterial>()
            .select(BaseMaterial::getMaterialCode, BaseMaterial::getMaterialName,
                BaseMaterial::getSpec, BaseMaterial::getBrand, BaseMaterial::getUnit));
        Map<String, String> index = new HashMap<>(rows.size());
        for (BaseMaterial row : rows) {
            index.putIfAbsent(fourElementKey(row.getMaterialName(), row.getSpec(), row.getBrand(), row.getUnit()),
                row.getMaterialCode());
        }
        return index;
    }

    /**
     * 四要素规范化键：名称|规格|品牌|单位，空值归一为空串（null 安全，杜绝字符串拼接出现 "null"）。
     *
     * @param materialName 物料名称
     * @param spec         规格型号
     * @param brand        品牌
     * @param unit         计量单位
     * @return 规范化查重键
     */
    public static String fourElementKey(String materialName, String spec, String brand, String unit) {
        return trimToEmpty(materialName) + "|"
            + trimToEmpty(spec) + "|"
            + trimToEmpty(brand) + "|"
            + trimToEmpty(unit);
    }

    /**
     * null 安全修剪（空值归一为空串）。
     */
    private static String trimToEmpty(String value) {
        return value == null ? "" : StringUtils.trimWhitespace(value);
    }
}
