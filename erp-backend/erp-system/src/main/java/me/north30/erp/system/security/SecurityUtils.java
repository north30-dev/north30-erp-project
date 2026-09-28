package me.north30.erp.system.security;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具：从 SecurityContextHolder 取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户（可能为 null：未认证/系统上下文）。
     */
    public static LoginUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    /**
     * 获取当前登录用户，无登录态时抛业务异常（10101）。
     */
    public static LoginUser requireCurrentUser() {
        LoginUser loginUser = getCurrentUser();
        if (loginUser == null) {
            throw new BusinessException(CommonErrorCode.AUTH_EXPIRED);
        }
        return loginUser;
    }
}
