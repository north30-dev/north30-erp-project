package me.north30.erp.system.auth.vo;

import java.util.List;

/**
 * 当前用户信息响应 VO（接口文档 4.5）。
 * 
 * @param userId 用户 ID
 * @param userCode 用户编码（登录名）
 * @param username 用户名（登录名）
 * @param realName 真实姓名
 * @param deptId 部门 ID
 * @param deptName 部门名称
 * @param phone 手机号
 * @param email 邮箱
 * @param warehouseIds 仓库 ID 列表
 * @param roles 角色列表
 * @param dataScope 数据范围（0：全部，1：本部门，2：本部门及以下部门）
 * @param lastLoginTime 最后登录时间
 * @param lastLoginIp 最后登录 IP
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
