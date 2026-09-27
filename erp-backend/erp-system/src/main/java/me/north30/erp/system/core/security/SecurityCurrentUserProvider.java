package me.north30.erp.system.core.security;

import me.north30.erp.common.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * 当前用户提供者实现：从 SecurityContextHolder 取当前登录用户，
 * 供 common 层（审计字段填充、审计切面等）解耦获取操作人。
 */
@Component
public class SecurityCurrentUserProvider implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        return loginUser != null ? loginUser.userId() : null;
    }

    @Override
    public String getCurrentUsername() {
        LoginUser loginUser = SecurityUtils.getCurrentUser();
        return loginUser != null ? loginUser.username() : null;
    }
}
