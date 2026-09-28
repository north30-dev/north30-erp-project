package me.north30.erp.system.config.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

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
}
