package me.north30.erp.system.core.vo;

/**
 * 组织部门写操作响应 VO（接口文档 5.4.2/5.4.3/5.4.4）。
 *
 * @param id 组织 ID
 * @param ancestors 服务端计算/重算后的祖级路径（删除时为 null）
 * @param deptLevel 服务端计算/重算后的层级（删除时为 null）
 * @param updateTime 更新时间（新增/删除时为 null）
 * @param isDeleted 逻辑删除标记（仅删除接口返回）
 */
public record DeptMutationVO(
    Long id,
    String ancestors,
    Integer deptLevel,
    String updateTime,
    Integer isDeleted
) {
}
