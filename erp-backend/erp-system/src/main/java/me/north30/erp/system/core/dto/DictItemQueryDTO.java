package me.north30.erp.system.core.dto;

import lombok.Data;

/**
 * 字典项分页查询参数（接口文档 5.5.5，按字典类型过滤）。
 */
@Data
public class DictItemQueryDTO {

    /** 字典类型编码（必填） */
    private String dictType;

    /** 语言，默认 zh-CN（E-03 预留） */
    private String lang;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 页码（从 1 开始） */
    private long pageNum = 1;

    /** 每页条数（上限 200） */
    private long pageSize = 20;
}
