package me.north30.erp.system.user.vo;

import java.util.List;

import lombok.Builder;

/**
 * 用户详情 VO（接口文档 5.1.2 出参：5.1.1 字段全集 + warehouseIds/锁定信息 + roleIds）。
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
 * @param roleIds 角色 ID 集合
 * @param warehouseIds 仓库 ID 集合
 * @param status 状态
 * @param isAdmin 是否是管理员
 * @param gender 性别
 * @param remark 备注
 * @param loginFailCount 登录失败次数
 * @param lockUntil 锁定时间
 * @param passwordUpdateTime 密码更新时间
 * @param lastLoginTime 最后登录时间
 * @param createTime 创建时间
 */
@Builder 
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
