package me.north30.erp.system.core.dto;

/**
 * 角色分页查询请求 DTO（接口文档 5.2.1，分页规范见 1.4）。
 */
public record RolePageQueryDTO(
    String roleCode,
    String roleName,
    Integer status,
    Integer pageNum,
    Integer pageSize,
    String orderBy,
    String orderDirection
) {
}
