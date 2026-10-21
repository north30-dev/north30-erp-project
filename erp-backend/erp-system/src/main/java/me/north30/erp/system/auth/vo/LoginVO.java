package me.north30.erp.system.auth.vo;

import java.util.List;

/**
 * 登录响应 VO（接口文档 4.2）。
 * 
 * @param accessToken 访问令牌（Bearer 格式）
 * @param tokenType 令牌类型（Bearer）
 * @param expiresIn 过期时间（秒）
 * @param refreshToken 刷新令牌（用于刷新新令牌）
 * @param userInfo 登录返回的用户信息
 */
public record LoginVO(
    String accessToken,
    String tokenType,
    Integer expiresIn,
    String refreshToken,
    LoginUserInfoVO userInfo
) {

    /**
     * 登录返回的用户信息。
     * 
     * @param userId 用户 ID
     * @param username 用户名（登录名）
     * @param realName 真实姓名
     * @param deptId 部门 ID
     * @param deptName 部门名称
     * @param roles 角色列表
     * @param isAdmin 是否管理员（0：否，1：是）
     * @param passwordExpired 密码是否过期（0：否，1：是）
     */
    public record LoginUserInfoVO(
        Long userId,
        String username,
        String realName,
        Long deptId,
        String deptName,
        List<String> roles,
        Integer isAdmin,
        Boolean passwordExpired
    ) {
    }
}
