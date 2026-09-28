package me.north30.erp.system.dept.dto;

import lombok.Data;

/**
 * 组织部门树查询参数（接口文档 5.4.1）。
 */
@Data
public class DeptTreeQueryDTO {

    /** 组织名称（模糊） */
    private String deptName;

    /** 类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间 */
    private Integer deptType;

    /** 状态 0-停用 1-启用 */
    private Integer status;
}
