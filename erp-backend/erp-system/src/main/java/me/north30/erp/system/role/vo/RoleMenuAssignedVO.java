package me.north30.erp.system.role.vo;

import java.util.List;

/**
 * 分配角色菜单权限响应 VO（接口文档 5.2.5，permCount 为授权含权限点的数量）。
 *
 * @param menuIds   菜单权限点 ID 集合
 * @param permCount 授权含权限点的数量
 */
public record RoleMenuAssignedVO(
    List<Long> menuIds,
    Integer permCount
) {
}
