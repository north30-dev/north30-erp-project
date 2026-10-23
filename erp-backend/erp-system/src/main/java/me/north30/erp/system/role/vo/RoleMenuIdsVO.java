package me.north30.erp.system.role.vo;

import java.util.List;

/**
 * 角色已分配菜单 ID 响应 VO（接口文档 5.2.8，供分配菜单弹窗回显勾选）。
 *
 * @param menuIds 已分配菜单权限点 ID 集合（sys_role_menu 逻辑未删除关联）
 */
public record RoleMenuIdsVO(
    List<Long> menuIds
) {
}
