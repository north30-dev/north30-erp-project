package me.north30.erp.system.role.vo;

/**
 * 角色分页记录响应 VO（接口文档 5.2.1，userCount 为 sys_user_role 派生计数）。
 */
public record RoleVO(
    Long id,
    String roleCode,
    String roleName,
    Integer roleSort,
    Integer dataScope,
    Integer isBuiltin,
    Integer status,
    Integer userCount
) {
}
