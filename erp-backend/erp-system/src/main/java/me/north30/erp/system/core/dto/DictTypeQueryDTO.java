package me.north30.erp.system.core.dto;

import lombok.Data;

/**
 * 字典类型分页查询参数（接口文档 5.5.1）。
 */
@Data
public class DictTypeQueryDTO {

    /** 字典类型编码（模糊） */
    private String dictType;

    /** 字典类型名称（模糊） */
    private String dictName;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 页码（从 1 开始） */
    private long pageNum = 1;

    /** 每页条数（上限 200） */
    private long pageSize = 20;
}
