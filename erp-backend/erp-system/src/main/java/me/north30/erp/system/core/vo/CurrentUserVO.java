package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 当前用户信息响应 VO（接口文档 4.5）。
 */
public record CurrentUserVO(
    Long userId,
    String userCode,
    String username,
    String realName,
    Long deptId,
    String deptName,
    String phone,
    String email,
    List<Long> warehouseIds,
    List<String> roles,
    Integer dataScope,
    String lastLoginTime,
    String lastLoginIp
) {
}
