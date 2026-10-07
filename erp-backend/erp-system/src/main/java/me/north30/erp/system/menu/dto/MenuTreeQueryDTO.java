package me.north30.erp.system.menu.dto;

/**
 * 菜单树查询请求 DTO（接口文档 5.3.1，条件均为可选）。
 *
 * @param menuName 菜单名称
 * @param perms    权限标识
 * @param status   状态 0-停用 1-启用
 */
public record MenuTreeQueryDTO(
    String menuName,
    String perms,
    Integer status
) {
}
