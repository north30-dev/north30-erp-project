package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysRole;

import java.util.Collection;
import java.util.List;

/**
 * 角色服务接口。
 */
public interface ISysRoleService {

    /**
     * 按主键查询。
     */
    SysRole getById(Long id);

    /**
     * 按角色编码查询（逻辑删除过滤）。
     */
    SysRole getByRoleCode(String roleCode);

    /**
     * 按主键集合批量查询。
     */
    List<SysRole> listByIds(Collection<Long> ids);

    /**
     * 新增角色。
     */
    boolean createRole(SysRole role);
}
