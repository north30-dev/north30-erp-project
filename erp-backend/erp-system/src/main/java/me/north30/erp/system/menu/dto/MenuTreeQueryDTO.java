package me.north30.erp.system.menu.dto;

/**
 * 菜单树查询请求 DTO（接口文档 5.3.1，条件均为可选）。
 */
public record MenuTreeQueryDTO(
    String menuName,
    String perms,
    Integer status
) {
}
