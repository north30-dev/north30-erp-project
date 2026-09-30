package me.north30.erp.system.user.vo;

import java.util.List;

/**
 * 用户列表项 VO（接口文档 5.1.1 出参，phone 掩码）。
 * 
 * @param id 用户 ID
 * @param userCode 用户编码
 * @param username 用户名
 * @param realName 真实姓名
 * @param phone 手机号
 * @param email 邮箱
 * @param deptId 部门 ID
 * @param deptName 部门名称
 * @param roles 角色列表
 * @param status 状态
 * @param isAdmin 是否是管理员
 * @param lastLoginTime 最后登录时间
 * @param createTime 创建时间
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
