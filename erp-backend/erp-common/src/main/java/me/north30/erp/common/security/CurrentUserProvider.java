package me.north30.erp.common.security;

/**
 * 当前用户提供者：common 模块不依赖 Spring Security，通过该接口解耦获取当前登录用户。
 * <p>实现类后续放在 erp-system 模块（从 SecurityContext 取人并注册为 Spring Bean）。
 * 未登录或系统上下文（如定时任务）中返回 null，由调用方自行处理。</p>
 */
public interface CurrentUserProvider {

    /**
     * 获取当前登录用户 ID
     * @return 用户 ID，无登录上下文时返回 null
     */
    Long getCurrentUserId();

    /**
     * 获取当前登录用户名
     * @return 用户名，无登录上下文时返回 null
     */
    String getCurrentUsername();
}
