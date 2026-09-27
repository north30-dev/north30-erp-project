package me.north30.erp.system.core.service;

import me.north30.erp.system.core.service.dto.UserSecurityData;

/**
 * 用户安全数据查询服务：认证/鉴权链路所需的账号状态、角色编码与权限点集合。
 * <p>admin（is_admin=1）返回全量权限点；普通用户按角色并集（sys_user_role → sys_role_menu → sys_menu.perms）。</p>
 */
public interface UserSecurityQueryService {

    /**
     * 按用户 ID 加载安全数据。
     *
     * @param userId 用户 ID
     * @return 安全数据；用户不存在时返回 null
     */
    UserSecurityData loadByUserId(Long userId);
}
