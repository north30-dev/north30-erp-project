package me.north30.erp.system.menu.vo;

/**
 * 菜单状态变更响应 VO（POST /api/system/menus/{id}/status 出参）。
 *
 * @param status 变更后状态
 */
public record MenuStatusVO(
    Integer status
) {
}
