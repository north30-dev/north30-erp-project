package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysUser;

import java.time.LocalDateTime;

/**
 * 用户服务接口。
 */
public interface ISysUserService {

    /**
     * 按用户名查询（逻辑删除过滤）。
     */
    SysUser getByUsername(String username);

    /**
     * 按主键查询。
     */
    SysUser getById(Long id);

    /**
     * 新增用户。
     */
    boolean createUser(SysUser user);

    /**
     * 更新用户。
     */
    boolean updateUser(SysUser user);

    /**
     * 登录成功后更新最后登录信息并清零失败计数。
     */
    void updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime);
}
