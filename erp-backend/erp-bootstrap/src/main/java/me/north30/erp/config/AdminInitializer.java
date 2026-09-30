package me.north30.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.user.converter.UserConverter;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.menu.service.SysMenuService;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.role.service.SysRoleService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.user.service.SysUserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员初始化器：应用启动时幂等创建超级管理员账号、内置角色与系统管理基础菜单。
 * <p>幂等策略：查无 admin 用户才执行创建；角色按 roleCode、菜单按 menuName 二次去重，
 * 中途失败重启可续建。口令为 BCrypt 密文，禁止明文入库。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    /** 初始管理员用户名 */
    private static final String ADMIN_USERNAME = "admin";
    /** 初始管理员口令（首次登录后应立即修改） */
    private static final String ADMIN_PASSWORD = "Admin@123456";
    /** 初始管理员用户编号 */
    private static final String ADMIN_USER_CODE = "ADMIN";
    /** 超级管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "admin";

    /** 菜单类型：目录 */
    private static final int TYPE_DIR = 1;
    /** 菜单类型：菜单 */
    private static final int TYPE_MENU = 2;
    /** 菜单类型：按钮 */
    private static final int TYPE_BUTTON = 3;

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final RoleConverter roleConverter;

    /**
     * 菜单种子节点：声明式描述菜单树，由 {@link #ensureTree} 递归落库。
     *
     * @param name      菜单名称（幂等键）
     * @param type      菜单类型 1 目录 / 2 菜单 / 3 按钮
     * @param path      前端路由（按钮为 null）
     * @param component 前端组件路径（目录/按钮为 null）
     * @param perms     权限标识（无则为 null）
     * @param icon      图标（按钮为 null）
     * @param sort      排序号
     * @param children  子节点
     */
    private record MenuSeed(String name, int type, String path, String component,
                            String perms, String icon, int sort, List<MenuSeed> children) {

        static MenuSeed dir(String name, String path, String icon, int sort, MenuSeed... children) {
            return new MenuSeed(name, TYPE_DIR, path, null, null, icon, sort, List.of(children));
        }

        static MenuSeed menu(String name, String path, String component, String perms,
                             String icon, int sort, MenuSeed... children) {
            return new MenuSeed(name, TYPE_MENU, path, component, perms, icon, sort, List.of(children));
        }

        static MenuSeed button(String name, String perms, int sort) {
            return new MenuSeed(name, TYPE_BUTTON, null, null, perms, null, sort, List.of());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void run(ApplicationArguments args) {
        if (sysUserService.getByUsername(ADMIN_USERNAME) != null) {
            log.info("管理员账号已存在，跳过初始化");
            return;
        }
        initialize();
    }

    /**
     * 执行初始化：角色 → 用户 → 用户角色关联 → 菜单树 → 角色菜单关联。
     */
    private void initialize() {
        boolean roleCreated = false;
        SysRole role = sysRoleService.getByRoleCode(ADMIN_ROLE_CODE);
        if (role == null) {
            role = new SysRole();
            role.setRoleCode(ADMIN_ROLE_CODE);
            role.setRoleName("超级管理员");
            role.setRoleSort(1);
            role.setDataScope(1);
            role.setIsBuiltin(1);
            role.setStatus(1);
            sysRoleService.createRole(role);
            roleCreated = true;
        }
        Long roleId = role.getId();

        SysUser user = new SysUser();
        user.setUserCode(ADMIN_USER_CODE);
        user.setUsername(ADMIN_USERNAME);
        user.setPassword(passwordEncoder.encode(ADMIN_PASSWORD));
        user.setRealName("系统管理员");
        user.setGender(0);
        user.setStatus(1);
        user.setIsAdmin(1);
        user.setLoginFailCount(0);
        user.setPasswordUpdateTime(LocalDateTime.now());
        sysUserService.createUser(user);

        sysUserRoleService.createUserRole(userConverter.toUserRole(user.getId(), roleId));

        List<SysMenu> allMenus = new ArrayList<>();
        List<SysMenu> createdMenus = new ArrayList<>();
        ensureTree(0L, menuTree(), allMenus, createdMenus);

        List<SysRoleMenu> roleMenus = new ArrayList<>();
        List<SysMenu> toGrant = roleCreated ? allMenus : createdMenus;
        for (SysMenu menu : toGrant) {
            roleMenus.add(roleConverter.toRoleMenu(roleId, menu.getId()));
        }
        if (!roleMenus.isEmpty()) {
            sysRoleMenuService.createBatch(roleMenus);
        }
        log.info("管理员初始化完成 | username: {} | 角色已创建: {} | 新增菜单数: {}",
                ADMIN_USERNAME, roleCreated, createdMenus.size());
    }

    /**
     * 系统管理菜单树定义（目录 → 菜单 → 按钮）。
     */
    private List<MenuSeed> menuTree() {
        return List.of(
            MenuSeed.dir("系统管理", "/system", "Setting", 1,
                MenuSeed.menu("用户管理", "/system/user", "system/user/index", "system:user:list", "User", 2,
                    MenuSeed.button("用户新增", "system:user:create", 1),
                    MenuSeed.button("用户修改", "system:user:update", 2),
                    MenuSeed.button("用户删除", "system:user:delete", 3)),
                MenuSeed.menu("角色管理", "/system/role", "system/role/index", "system:role:list", "Team", 2,
                    MenuSeed.button("角色新增", "system:role:create", 1),
                    MenuSeed.button("角色修改", "system:role:update", 2),
                    MenuSeed.button("角色删除", "system:role:delete", 3)),
                MenuSeed.menu("菜单管理", "/system/menu", "system/menu/index", "system:menu:list", "Menu", 3,
                    MenuSeed.button("菜单新增", "system:menu:create", 1),
                    MenuSeed.button("菜单修改", "system:menu:update", 2),
                    MenuSeed.button("菜单删除", "system:menu:delete", 3)),
                MenuSeed.menu("组织管理", "/system/dept", "system/dept/index", "system:dept:list", "Apartment", 4),
                MenuSeed.menu("字典管理", "/system/dict", "system/dict/index", "system:dict:list", "Book", 5),
                MenuSeed.menu("参数管理", "/system/config", "system/config/index", "system:config:list", "Tool", 6),
                MenuSeed.menu("登录日志", "/system/login-log", "system/loginLog/index", "system:loginLog:list", "FileText", 7)));
    }

    /**
     * 递归落库菜单树：按名称幂等，收集全部节点与本次新增节点。
     */
    private void ensureTree(Long parentId, List<MenuSeed> seeds, List<SysMenu> allMenus, List<SysMenu> createdMenus) {
        for (MenuSeed seed : seeds) {
            SysMenu menu = sysMenuService.getByMenuName(seed.name());
            if (menu == null) {
                menu = new SysMenu();
                menu.setMenuName(seed.name());
                menu.setParentId(parentId);
                menu.setMenuType(seed.type());
                menu.setPath(seed.path());
                menu.setComponent(seed.component());
                menu.setPerms(seed.perms());
                menu.setIcon(seed.icon());
                menu.setMenuSort(seed.sort());
                menu.setVisible(seed.type() == TYPE_BUTTON ? 0 : 1);
                menu.setStatus(1);
                sysMenuService.createMenu(menu);
                createdMenus.add(menu);
            }
            allMenus.add(menu);
            ensureTree(menu.getId(), seed.children(), allMenus, createdMenus);
        }
    }
}
