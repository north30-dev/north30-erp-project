package me.north30.erp.system.menu.service;

import me.north30.erp.system.menu.entity.SysMenu;

import java.util.Collection;
import java.util.List;

/**
 * 菜单与权限点服务接口。
 */
public interface SysMenuDomainService {

    /**
     * 按菜单名称查询（初始化幂等用）。
     * 
     * @param menuName 菜单名称
     * @return 菜单实体
     */
    SysMenu getByMenuName(String menuName);

    /**
     * 查询全部启用菜单（单次查询，内存组树防 N+1）。
     * 
     * @return 启用菜单列表
     */
    List<SysMenu> listEnabled();

    /**
     * 按主键集合批量查询启用菜单。 
     * 
     * @param ids 主键集合
     * @return 启用菜单列表
     */
    List<SysMenu> listEnabledByIds(Collection<Long> ids);

    /**
     * 按主键集合批量查询菜单（不限状态，存在性校验用）。
     * 
     * @param ids 主键集合
     * @return 菜单列表
     */
    List<SysMenu> listByIds(Collection<Long> ids);

    /**
     * 查询全部启用且配置了权限点的菜单权限标识（admin 全量权限点用）。
     * 
     * @return 权限标识列表
     */
    List<String> listAllPerms();

    /**
     * 新增菜单。
     * 
     * @param menu 菜单实体
     * @return 是否新增成功
     */
    boolean createMenu(SysMenu menu);

    /**
     * 更新菜单权限标识（AdminInitializer 幂等续建时同步种子与库内 perms 差异）。
     * 
     * @param menuId 菜单 ID
     * @param perms 权限标识
     */
    void updatePerms(Long menuId, String perms);

    /**
     * 校验菜单存在，不存在抛 19002（@TableLogic 自动过滤已删除行）。
     * @param id 菜单 ID
     * @return 菜单实体
     */
    SysMenu requireMenu(Long id);

    /**
     * perms 唯一守卫（excludeId 用于修改场景排除自身），重复抛 MENU_PERMS_DUPLICATED；
     * perms 空白直接放行。
     * @param perms 权限标识
     * @param excludeId 排除的菜单 ID（修改场景传自身 ID，新增传 null）
     */
    void requirePermsAvailable(String perms, Long excludeId);

    /**
     * 父级合法性守卫：存在性 + 非按钮 + 防环（逐级向上回溯）。
     * <p>parentId 为 null 或 0（顶级）直接放行；父不存在抛 MENU_NOT_FOUND；
     * 父为按钮抛 MENU_LEVEL_EXCEEDED；自指/成环抛 PARAM_ERROR。
     * selfId 为 null 表示新增场景（无自身 ID，跳过防环回溯）。</p>
     * @param selfId 当前菜单 ID（新增场景传 null）
     * @param parentId 目标父级菜单 ID
     */
    void requireValidParent(Long selfId, Long parentId);
}
