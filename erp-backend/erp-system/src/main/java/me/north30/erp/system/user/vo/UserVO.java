package me.north30.erp.system.user.vo;

import java.util.List;

/**
 * 用户列表项 VO（接口文档 5.1.1 出参，phone 掩码）。
 */
public record UserVO(
    Long id,
    String userCode,
    String username,
    String realName,
    String phone,
    String email,
    Long deptId,
    String deptName,
    List<String> roles,
    Integer status,
    Integer isAdmin,
    String lastLoginTime,
    String createTime
) {
}
