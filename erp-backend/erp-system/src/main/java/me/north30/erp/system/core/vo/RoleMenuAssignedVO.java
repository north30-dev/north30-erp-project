package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 分配角色菜单权限响应 VO（接口文档 5.2.5，permCount 为授权含权限点的数量）。
 */
public record RoleMenuAssignedVO(
    List<Long> menuIds,
    Integer permCount
) {
}
