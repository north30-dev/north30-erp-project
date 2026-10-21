package me.north30.erp.system.auth.dto;

import java.util.List;

/**
 * 用户安全数据：认证转换器与权限接口所需的账号状态、角色、权限点与数据范围档位聚合数据。
 *
 * @param userId 用户 ID
 * @param status 账号状态 0-停用 1-启用
 * @param isAdmin 是否超级管理员 0-否 1-是
 * @param roleCodes 角色编码集合（仅启用角色）
 * @param perms 权限点集合（admin 为全量）
 * @param widestDataScope 最宽数据范围档位（多角色取最宽：1-全部 > 2 > 3 > 4 > 5 > 6 > 9-自定义；无角色按"仅本人"6）
 */
public record UserSecurityData(
    Long userId,
    Integer status,
    Integer isAdmin,
    List<String> roleCodes,
    List<String> perms,
    Integer widestDataScope
) {
}
