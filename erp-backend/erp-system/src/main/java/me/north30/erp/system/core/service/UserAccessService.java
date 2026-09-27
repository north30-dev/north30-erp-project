package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysUser;
import me.north30.erp.system.core.service.dto.UserSecurityData;
import me.north30.erp.system.core.vo.MenuTreeVO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * 用户访问聚合服务：认证/鉴权链路所需的账号读取与更新、部门名称、角色编码、菜单树、
 * 权限点集合与数据范围档位的统一取数入口（认证服务不再直接编排各 Sys* 服务）。
 * <p>admin（is_admin=1）返回全量权限点与全量菜单；普通用户按启用角色并集
 * （sys_user_role → sys_role_menu → sys_menu）取数，一次批量查出后内存组装，无循环查库。</p>
 */
public interface UserAccessService {

    /**
     * 按用户 ID 加载安全数据（账号状态、角色编码、权限点集合、最宽数据范围档位）。
     *
     * @param userId 用户 ID
     * @return 安全数据；用户不存在时返回 null
     */
    UserSecurityData loadByUserId(Long userId);

    /**
     * 按用户名查询账号（登录认证用，逻辑删除过滤）。
     */
    SysUser getByUsername(String username);

    /**
     * 按用户 ID 查询账号（刷新令牌、当前用户信息、菜单树与改密场景）。
     */
    SysUser getUserById(Long userId);

    /**
     * 登录成功后更新最后登录信息（委托用户服务写事务）。
     */
    void updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime);

    /**
     * 更新用户（口令修改场景，委托用户服务写事务）。
     */
    void updateUser(SysUser user);

    /**
     * 查询部门名称（部门不存在或 ID 为空返回 null）。
     */
    String getDeptName(Long deptId);

    /**
     * 查询用户启用角色编码集合（两次批量查询，无循环查库）。
     */
    List<String> listRoleCodes(Long userId);

    /**
     * 查询用户可见菜单树（目录/菜单两级，children 递归）。
     * <p>admin 返回全部启用菜单组树；普通用户按启用角色并集取菜单，
     * 一次查全量启用菜单在内存补齐父级链（角色可能只勾选叶子菜单）。</p>
     *
     * @param userId  用户 ID
     * @param isAdmin 是否超级管理员
     * @return 树形菜单列表
     */
    List<MenuTreeVO> listMenuTree(Long userId, boolean isAdmin);

    /**
     * 查询用户权限点集合。
     * <p>admin 返回全量权限点；普通用户按启用角色并集（sys_role_menu → sys_menu.perms）。</p>
     *
     * @param userId  用户 ID
     * @param isAdmin 是否超级管理员
     * @return 权限点集合（去重、去空白）
     */
    Set<String> listPerms(Long userId, boolean isAdmin);
}
