package me.north30.erp.system.core.service.dto;

import java.util.List;

/**
 * 用户安全数据：认证转换器与权限接口所需的账号状态、角色、权限点集合。
 *
 * @param userId 用户 ID
 * @param status 账号状态 0-停用 1-启用
 * @param isAdmin 是否超级管理员 0-否 1-是
 * @param roleCodes 角色编码集合
 * @param perms 权限点集合（admin 为全量）
 */
public record UserSecurityData(
    Long userId,
    Integer status,
    Integer isAdmin,
    List<String> roleCodes,
    List<String> perms
) {
}
