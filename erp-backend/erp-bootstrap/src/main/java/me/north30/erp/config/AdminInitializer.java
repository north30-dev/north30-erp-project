package me.north30.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.role.entity.SysUserRole;
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

    private final SysUserService sysUserService;
    private final SysRoleService sysRoleService;
    private final SysMenuService sysMenuService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final PasswordEncoder passwordEncoder;

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

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(roleId);
        sysUserRoleService.createUserRole(userRole);

        List<SysMenu> allMenus = new ArrayList<>();
        List<SysMenu> createdMenus = new ArrayList<>();
        ensureMenus(allMenus, createdMenus);
        List<SysRoleMenu> roleMenus = new ArrayList<>();
        List<SysMenu> toGrant = roleCreated ? allMenus : createdMenus;
        for (SysMenu menu : toGrant) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(roleId);
            roleMenu.setMenuId(menu.getId());
            roleMenus.add(roleMenu);
        }
        if (!roleMenus.isEmpty()) {
            sysRoleMenuService.createBatch(roleMenus);
        }
        log.info("管理员初始化完成 | username: {} | 角色已创建: {} | 新增菜单数: {}",
                ADMIN_USERNAME, roleCreated, createdMenus.size());
    }

    /**
     * 幂等创建系统管理菜单树（目录 → 菜单 → 按钮），收集全部与本次新增的菜单。
     */
    private void ensureMenus(List<SysMenu> allMenus, List<SysMenu> createdMenus) {
        Long systemDirId = ensureMenu("系统管理", 0L, 1, "/system", null, null, "Setting", 1,
                allMenus, createdMenus);

        Long userId = ensureMenu("用户管理", systemDirId, 2, "/system/user", "system/user/index",
                "system:user:list", "User", 1, allMenus, createdMenus);
        ensureButton("用户新增", userId, "system:user:create", 1, allMenus, createdMenus);
        ensureButton("用户修改", userId, "system:user:update", 2, allMenus, createdMenus);
        ensureButton("用户删除", userId, "system:user:delete", 3, allMenus, createdMenus);

        Long roleIdMenu = ensureMenu("角色管理", systemDirId, 2, "/system/role", "system/role/index",
                "system:role:list", "Team", 2, allMenus, createdMenus);
        ensureButton("角色新增", roleIdMenu, "system:role:create", 1, allMenus, createdMenus);
        ensureButton("角色修改", roleIdMenu, "system:role:update", 2, allMenus, createdMenus);
        ensureButton("角色删除", roleIdMenu, "system:role:delete", 3, allMenus, createdMenus);

        Long menuId = ensureMenu("菜单管理", systemDirId, 2, "/system/menu", "system/menu/index",
                "system:menu:list", "Menu", 3, allMenus, createdMenus);
        ensureButton("菜单新增", menuId, "system:menu:create", 1, allMenus, createdMenus);
        ensureButton("菜单修改", menuId, "system:menu:update", 2, allMenus, createdMenus);
        ensureButton("菜单删除", menuId, "system:menu:delete", 3, allMenus, createdMenus);

        ensureMenu("组织管理", systemDirId, 2, "/system/dept", "system/dept/index",
                "system:dept:list", "Apartment", 4, allMenus, createdMenus);
        ensureMenu("字典管理", systemDirId, 2, "/system/dict", "system/dict/index",
                "system:dict:list", "Book", 5, allMenus, createdMenus);
        ensureMenu("参数管理", systemDirId, 2, "/system/config", "system/config/index",
                "system:config:list", "Tool", 6, allMenus, createdMenus);
        ensureMenu("登录日志", systemDirId, 2, "/system/login-log", "system/loginLog/index",
                "system:loginLog:list", "FileText", 7, allMenus, createdMenus);
    }

    /**
     * 按菜单名称幂等创建单个菜单，返回菜单 ID。
     */
    private Long ensureMenu(String menuName, Long parentId, Integer menuType, String path,
                            String component, String perms, String icon, Integer menuSort,
                            List<SysMenu> allMenus, List<SysMenu> createdMenus) {
        SysMenu existing = sysMenuService.getByMenuName(menuName);
        if (existing != null) {
            allMenus.add(existing);
            return existing.getId();
        }
        SysMenu menu = new SysMenu();
        menu.setMenuName(menuName);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        menu.setPath(path);
        menu.setComponent(component);
        menu.setPerms(perms);
        menu.setIcon(icon);
        menu.setMenuSort(menuSort);
        menu.setVisible(1);
        menu.setStatus(1);
        sysMenuService.createMenu(menu);
        allMenus.add(menu);
        createdMenus.add(menu);
        return menu.getId();
    }

    /**
     * 幂等创建按钮权限点（menuType=3，不可见）。
     */
    private void ensureButton(String menuName, Long parentId, String perms, Integer menuSort,
                              List<SysMenu> allMenus, List<SysMenu> createdMenus) {
        ensureMenu(menuName, parentId, 3, null, null, perms, null, menuSort, allMenus, createdMenus);
    }
}
