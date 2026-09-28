package me.north30.erp.system.menu.service;

import me.north30.erp.system.menu.entity.SysMenu;

import java.util.Collection;
import java.util.List;

/**
 * 菜单与权限点服务接口。
 */
public interface SysMenuService {

    /**
     * 按菜单名称查询（初始化幂等用）。
     */
    SysMenu getByMenuName(String menuName);

    /**
     * 查询全部启用菜单（单次查询，内存组树防 N+1）。
     */
    List<SysMenu> listEnabled();

    /**
     * 按主键集合批量查询启用菜单。
     */
    List<SysMenu> listEnabledByIds(Collection<Long> ids);

    /**
     * 查询全部启用且配置了权限点的菜单权限标识（admin 全量权限点用）。
     */
    List<String> listAllPerms();

    /**
     * 新增菜单。
     */
    boolean createMenu(SysMenu menu);
}
