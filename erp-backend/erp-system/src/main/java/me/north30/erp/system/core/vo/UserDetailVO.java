package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 用户详情 VO（接口文档 5.1.2 出参：5.1.1 字段全集 + warehouseIds/锁定信息 + roleIds）。
 */
public record UserDetailVO(
    Long id,
    String userCode,
    String username,
    String realName,
    String phone,
    String email,
    Long deptId,
    String deptName,
    List<String> roles,
    List<Long> roleIds,
    List<Long> warehouseIds,
    Integer status,
    Integer isAdmin,
    Integer gender,
    String remark,
    Integer loginFailCount,
    String lockUntil,
    String passwordUpdateTime,
    String lastLoginTime,
    String createTime
) {
}
