package me.north30.erp.system.role.service;

import me.north30.erp.system.role.entity.SysRoleMenu;

import java.util.Collection;
import java.util.List;

/**
 * 角色-菜单关联服务接口。
 */
public interface SysRoleMenuService {

    /**
     * 按角色 ID 集合查询关联。
     * @param roleIds 角色 ID 集合
     * @return 角色-菜单关联列表
     */
    List<SysRoleMenu> listByRoleIds(Collection<Long> roleIds);

    /**
     * 新增关联。
     * @param roleMenu 角色-菜单关联
     * @return 是否创建成功
     */
    boolean createRoleMenu(SysRoleMenu roleMenu);

    /**
     * 批量新增关联（初始化授权用）。
     * @param roleMenus 角色-菜单关联列表
     * @param roleIds 角色 ID 集合
     * @param menuIds 菜单 ID 集合
    * @return 是否创建成功
     */
    void createBatch(List<SysRoleMenu> roleMenus);

    /**
     * 判断菜单是否已被任何角色引用。
     * @param menuId 菜单 ID
     * @return 存在角色引用返回 true
     */
    boolean existsByMenuId(Long menuId);
}
