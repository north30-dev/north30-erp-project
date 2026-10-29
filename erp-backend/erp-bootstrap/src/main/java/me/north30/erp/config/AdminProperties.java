package me.north30.erp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 管理员初始化配置项（application.yml 的 erp.admin 前缀）。
 * <p>仅影响 AdminInitializer 首次创建账号时的用户名与初始口令；
 * 账号已存在时修改配置不会重置口令（口令归用户管理功能管辖）。</p>
 *
 * @param superAdmin  超级管理员初始账号（绑定 ADMIN 角色，is_admin=1）
 * @param systemAdmin 系统管理员初始账号（绑定 SYSTEM_ADMIN 角色，is_admin=0）
 */
@ConfigurationProperties(prefix = "erp.admin")
public record AdminProperties(AdminAccount superAdmin, AdminAccount systemAdmin) {

    /**
     * 单个初始账号。
     *
     * @param username 登录用户名
     * @param password 初始明文口令（BCrypt 加密后入库，生产环境应通过环境变量覆盖）
     */
    public record AdminAccount(String username, String password) {

        /** 未配置时返回 null（由调用方判定并告警/兜底） */
        public boolean isBlank() {
            return username == null || username.isBlank()
                    || password == null || password.isBlank();
        }
    }
}
