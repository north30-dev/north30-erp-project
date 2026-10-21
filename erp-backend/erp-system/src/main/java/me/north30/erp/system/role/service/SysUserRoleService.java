package me.north30.erp.system.role.service;

import me.north30.erp.system.role.entity.SysUserRole;

import java.util.Collection;
import java.util.List;

/**
 * 用户-角色关联服务接口。
 */
public interface SysUserRoleService {

    /**
     * 按用户 ID 查询关联。
     * @param userId 用户 ID
     * @return 用户-角色关联列表
     */
    List<SysUserRole> listByUserId(Long userId);

    /**
     * 新增关联。
     * @param userRole 用户-角色关联
     * @return 是否创建成功
     */
    boolean createUserRole(SysUserRole userRole);

    /**
     * 按角色 ID 集合查询关联（分页 userCount 派生用，单次 IN 查询）。
     * @param roleIds 角色 ID 集合
     * @return 用户-角色关联列表
     */
    List<SysUserRole> listByRoleIds(Collection<Long> roleIds);

    /**
     * 统计分配到指定角色的用户数。
     * @param roleId 角色 ID
     * @return 用户数
     */
    long countUsersByRoleId(Long roleId);

    /**
     * 统计用户已分配角色数（删除用户前置引用守卫用）。
     * @param userId 用户 ID
     * @return 角色 ID 集合数量
     */
    long countByUserId(Long userId);

    /**
     * 查询用户已分配角色 ID 列表（详情/装配用）。
     * @param userId 用户 ID
     * @return 角色 ID 集合
     */
    List<Long> listRoleIdsByUserId(Long userId);

    /**
     * 为用户逐条写入角色关联（实体由本域构建）。
     * @param userId 用户 ID
     * @param roleId 角色 ID
     */
    void createForUser(Long userId, Long roleId);

    /**
     * 逻辑删除用户全部角色关联（全删全插前半段）。
     * @param userId 用户 ID
     */
    void deleteByUserId(Long userId);
}
