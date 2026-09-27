package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 登录响应 VO（接口文档 4.2）。
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
