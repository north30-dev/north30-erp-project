package me.north30.erp.system.dept.dto;

/**
 * 组织部门树查询参数（接口文档 5.4.1）。
 */
public record DeptTreeQueryDTO(
    String deptName,
    Integer deptType,
    Integer status
) {
}
