package me.north30.erp.system.role.service;

import me.north30.erp.system.role.entity.SysRole;

import java.util.Collection;
import java.util.List;

/**
 * 角色服务接口。
 */
public interface SysRoleService {

    /**
     * 按主键查询角色。
     * @param id 角色 ID
     * @return 角色实体
     */
    SysRole getById(Long id);

    /**
     * 按角色编码查询（逻辑删除过滤）。 
     * @param roleCode 角色编码
     * @return 角色实体
     */
    SysRole getByRoleCode(String roleCode);

    /**
     * 按主键集合批量查询角色。
     * @param ids 角色 ID 集合
     * @return 角色实体列表
     */
    List<SysRole> listByIds(Collection<Long> ids);

    /**
     * 新增角色。
     * @param role 角色实体
     * @return 是否新增成功
     */
    boolean createRole(SysRole role);

    /**
     * 校验角色存在，不存在抛 18018（@TableLogic 自动过滤已删除行）。
     * @param id 角色 ID
     * @return 角色实体
     */
    SysRole requireRole(Long id);
}
