package me.north30.erp.system.dept.dto;

/**
 * 组织部门树查询参数（接口文档 5.4.1）。
 *
 * @param deptName 组织名称
 * @param deptType 组织类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间
 * @param status   状态 0-停用 1-启用
 */
public record DeptTreeQueryDTO(
    String deptName,
    Integer deptType,
    Integer status
) {
}
