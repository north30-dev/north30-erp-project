package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysRoleMenu;

import java.util.Collection;
import java.util.List;

/**
 * 角色-菜单关联服务接口。
 */
public interface ISysRoleMenuService {

    /**
     * 按角色 ID 集合查询关联。
     */
    List<SysRoleMenu> listByRoleIds(Collection<Long> roleIds);

    /**
     * 新增关联。
     */
    boolean createRoleMenu(SysRoleMenu roleMenu);

    /**
     * 批量新增关联（初始化授权用）。
     */
    void createBatch(List<SysRoleMenu> roleMenus);
}
