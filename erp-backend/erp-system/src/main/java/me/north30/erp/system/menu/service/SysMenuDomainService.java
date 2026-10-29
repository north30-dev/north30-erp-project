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
     * 查询指定父级下的同级菜单（sort ASC, id ASC，move 规范化重排用）。
     *
     * @param parentId 父级菜单 ID（顶级传 null 或 0）
     * @return 同级菜单列表
     */
    List<SysMenu> listSiblings(Long parentId);

    /**
     * 计算同级下一个显示顺序（当前同级 max(menuSort) + 10，空同级返回 10）。
     * <p>新增菜单未显式指定 menuSort 时调用（08 文档阶段四排序统一）。</p>
     *
     * @param parentId 父级菜单 ID（顶级传 null 或 0）
     * @return 下一个显示顺序值
     */
    int nextMenuSort(Long parentId);

    /**
     * 查询全量菜单（含停用，超管菜单树专用——自锁兜底，08 文档 2.3）。
     *
     * @return 全量菜单列表
     */
    List<SysMenu> listAll();

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
     * 标记菜单为内置（is_system=1）：种子菜单落库时写入；已有行未标记时补标。
     *
     * @param menu 已加载的菜单实体（原地置 isSystem=1 后更新）
     */
    void markSystem(SysMenu menu);

    /**
     * 内置菜单停用级联守卫：目标节点或其子孙含 is_system=1 时拒绝停用（18039）。
     * <p>仅停用场景调用（启用无需拦截）；一次全量查询后内存判定，禁止循环查库。</p>
     *
     * @param menuId 待停用的菜单 ID
     */
    void requireSubtreeNotSystem(Long menuId);

    /**
     * 启动复位：is_system=1 且 status=0 的内置菜单强制恢复启用（08 文档 2.4 万能钥匙兜底）。
     *
     * @return 复位行数
     */
    int resetSystemMenus();

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
     * 父级结构合法性守卫：父级存在性 + 父子类型配对（委托 {@link SysMenu#requireValidChildType}）。
     * <p>create/update 共用；parentId 为 null 或 0（顶级）时仅校验 childType 非按钮（18023/10001）；
     * 父不存在抛 MENU_NOT_FOUND（18019）。不含自指/防环（update 换父级场景由
     * {@link #requireNotDescendant} 承载），不含深度校验（由 requireDepthWithinLimit 承载）。</p>
     * @param parentId 目标父级菜单 ID（顶级传 null 或 0）
     * @param childType 待挂载的子节点菜单类型
     */
    void requireValidParent(Long parentId, Integer childType);


    /**
     * 修改换父级时防环守卫：修改后的父级不可为自身或自身子孙。
     * <p>仅 update 换父级场景由编排层调用。父级为自身或自身子孙抛 PARAM_ERROR。
     * 沿新父级向上回溯，若回到自身则说明新父级为自身子孙，抛 PARAM_ERROR。</p>
     *
     * @param selfId 当前菜单 ID
     * @param newParentId 目标父级菜单 ID
     */
    void requireNotDescendant(Long selfId, Long newParentId);

    /**
     * 深度守卫（新增场景）：父级深度 + 1 ≤ {@link SysMenu#MAX_DEPTH}。
     * <p>顶级（parentId 为 null 或 0）子节点深度恒为 1，直接放行不查库；
     * 超限抛 MENU_LEVEL_EXCEEDED（18023）。一次全量查询后内存计算。</p>
     * @param parentId 目标父级菜单 ID
     * @param childType 待挂载的子节点菜单类型
     */
    void requireDepthWithinLimit(Long parentId, Integer childType);

    /**
     * 深度守卫（修改换父级场景）：新父级深度 + 1 + 自身子树高度 − 1 ≤ {@link SysMenu#MAX_DEPTH}。
     * <p>仅 update 换父级时由编排层调用（与 requireNotDescendant 同场景）；
     * 新父级不存在按父级深度 0 处理；超限抛 MENU_LEVEL_EXCEEDED（18023）。</p>
     * @param selfId 当前菜单 ID
     * @param newParentId 目标父级菜单 ID
     */
    void requireSubtreeDepthWithinLimit(Long selfId, Long newParentId);
}
