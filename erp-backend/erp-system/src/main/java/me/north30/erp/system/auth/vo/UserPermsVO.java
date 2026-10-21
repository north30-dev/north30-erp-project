package me.north30.erp.system.auth.vo;

import java.util.List;

/**
 * 当前用户权限点集合响应 VO（接口文档 4.7）。
 * 
 * @param perms 权限点列表
 * @param roles 角色列表
 */
public record UserPermsVO(
    List<String> perms,
    List<String> roles
) {
}
