package me.north30.erp.system.core.dto;

import lombok.Data;

/**
 * 系统参数分页查询参数（接口文档 5.6.1）。
 */
@Data
public class ConfigQueryDTO {

    /** 参数键（模糊） */
    private String configKey;

    /** 参数名称（模糊） */
    private String configName;

    /** 分组 SYSTEM/INVENTORY/PURCHASE/SALES/FINANCE/MANUFACTURING */
    private String configGroup;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 页码（从 1 开始） */
    private long pageNum = 1;

    /** 每页条数（上限 200） */
    private long pageSize = 20;
}
