package me.north30.erp.system.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 系统参数表（sys_config）：阈值/容差/天数集中维护，修改后 ≤1 分钟生效。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_config")
public class SysConfig extends BaseEntity {

    /** 参数键（如 inventory.stagnant.days，全局唯一） */
    private String configKey;

    /** 参数名称 */
    private String configName;

    /** 参数值 */
    private String configValue;

    /** 值类型 1-字符串 2-数字 3-布尔 4-JSON */
    private Integer valueType;

    /** 分组 SYSTEM/INVENTORY/PURCHASE/SALES/FINANCE/MANUFACTURING */
    private String configGroup;

    /** 是否系统内置 0-否 1-是（内置不可删） */
    private Integer isSystem;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 值类型编码 → 名称（接口文档 5.6） */
    private static final Map<Integer, String> VALUE_TYPE_NAMES = Map.of(
        1, "字符串", 2, "数字", 3, "布尔", 4, "JSON");

    /**
     * 按 valueType 校验参数值：2-数字须可解析为 BigDecimal；3-布尔仅允许 true/false；4-JSON 须可解析。
     * <p>充血方法：仅校验自身字段（valueType/configValue），ObjectMapper 作为外部能力由调用方传入。</p>
     * 
     * @param objectMapper JSON 解析器
     */
    public void validateValue(ObjectMapper objectMapper) {
        String typeName = VALUE_TYPE_NAMES.get(valueType);
        if (typeName == null) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "参数值类型 " + valueType + " 非法");
        }
        switch (valueType) {
            case 2 -> {
                try {
                    new BigDecimal(configValue);
                } catch (NumberFormatException e) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            case 3 -> {
                if (!"true".equalsIgnoreCase(configValue) && !"false".equalsIgnoreCase(configValue)) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            case 4 -> {
                try {
                    objectMapper.readTree(configValue);
                } catch (JacksonException e) {
                    throw new BusinessException(SystemManageErrorCode.CONFIG_VALUE_TYPE_MISMATCH,
                        "参数值类型错误，期望类型 " + typeName);
                }
            }
            default -> {
                // 1-字符串：不做格式校验
            }
        }
    }
}
