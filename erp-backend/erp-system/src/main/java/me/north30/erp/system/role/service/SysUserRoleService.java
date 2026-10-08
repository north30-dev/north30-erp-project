package me.north30.erp.system.role.service;

import me.north30.erp.system.role.entity.SysUserRole;

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
}
