package me.north30.erp.system.user.service;

import me.north30.erp.system.user.entity.SysUser;

import java.time.LocalDateTime;

/**
 * 用户服务接口。
 */
public interface SysUserService {

    /**
     * 按用户名查询（逻辑删除过滤）。
     * @param username 用户名
     * @return 用户实体
     */
    SysUser getByUsername(String username);

    /**
     * 按主键查询。
     * @param id 用户 ID
     * @return 用户实体
     */
    SysUser getById(Long id);

    /**
     * 校验用户存在，不存在抛 18005（@TableLogic 自动过滤已删除行）。
     * @param id 用户 ID
     * @return 用户实体
     */
    SysUser requireUser(Long id);

    /**
     * 校验用户名可用（全局唯一），已存在抛 18006。
     * @param username 用户名
     */
    void requireUsernameAvailable(String username);

    /**
     * 校验用户编号可用（全局唯一），已存在抛 18007。
     * @param userCode 用户编号
     */
    void requireUserCodeAvailable(String userCode);

    /**
     * 统计挂靠在指定部门下的用户数（sys_user.dept_id，逻辑删除过滤）。
     * @param deptId 部门 ID
     * @return 用户数
     */
    long countByDeptId(Long deptId);

    /**
     * 新增用户。
     * @param user 用户实体
     * @return 是否新增成功
     */
    boolean createUser(SysUser user);

    /**
     * 更新用户。
     * @param user 用户实体
     * @return 是否更新成功
     */
    boolean updateUser(SysUser user);

    /**
     * 登录成功后更新最后登录信息并清零失败计数。
     * @param userId 用户 ID
     * @param loginIp 登录 IP
     * @param loginTime 登录时间
     */
    void updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime);
}
