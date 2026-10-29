package me.north30.erp.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.role.service.SysRoleDomainService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.user.service.SysUserDomainService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 管理员初始化器：应用启动时幂等创建超级管理员与系统管理员两个内置账号，
 * 以及系统管理基础菜单。ADMIN / SYSTEM_ADMIN 内置角色由 seed.sql 种子脚本提供。
 * <p>幂等策略：账号按 username、角色按 roleCode、菜单按 menuName 幂等；
 * 中途失败重启可续建。口令为 BCrypt 密文，初始口令由 erp.admin 配置注入，禁止明文入库。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements ApplicationRunner {

    /** 默认初始口令（检测到未覆盖时告警，生产环境应通过环境变量注入） */
    private static final String DEFAULT_PASSWORD = "Admin@123456";
    /** 超级管理员用户编号 */
    private static final String ADMIN_USER_CODE = "ADMIN";
    /** 系统管理员用户编号 */
    private static final String SYSTEM_ADMIN_USER_CODE = "SYSADMIN";
    /** 超级管理员角色编码 */
    private static final String ADMIN_ROLE_CODE = "ADMIN";
    /** 系统管理员内置角色编码（seed.sql 种子提供，预勾选全部 /system 菜单，权限集可由超管调整） */
    private static final String SYSTEM_ADMIN_ROLE_CODE = "SYSTEM_ADMIN";

    /** 菜单类型：目录 */
    private static final int TYPE_DIR = 1;
    /** 菜单类型：菜单 */
    private static final int TYPE_MENU = 2;
    /** 菜单类型：按钮 */
    private static final int TYPE_BUTTON = 3;

    private final SysUserDomainService sysUserDomainService;
    private final SysRoleDomainService sysRoleDomainService;
    private final SysMenuDomainService sysMenuDomainService;
    private final SysUserRoleService sysUserRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final PasswordEncoder passwordEncoder;
    private final RoleConverter roleConverter;
    private final AdminProperties adminProperties;

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
        // 启动复位（08 文档 2.4 万能钥匙兜底）：内置菜单强制恢复启用，防误停导致管理端不可用
        int resetCount = sysMenuDomainService.resetSystemMenus();
        if (resetCount > 0) {
            log.warn("已复位 {} 个被停用的内置菜单", resetCount);
        }

        // ADMIN 角色：由 seed.sql 种子提供；查无时兜底创建（引导账号登录强依赖，不可缺席）
        SysRole adminRole = sysRoleDomainService.getByRoleCode(ADMIN_ROLE_CODE);
        if (adminRole == null) {
            adminRole = createAdminRole();
        }
        // SYSTEM_ADMIN 角色：仅由 seed.sql 种子提供，查无时告警并跳过（不在代码里兜底造角色）
        SysRole systemAdminRole = sysRoleDomainService.getByRoleCode(SYSTEM_ADMIN_ROLE_CODE);
        if (systemAdminRole == null) {
            log.warn("内置角色 {} 未由种子脚本创建（seed.sql 未执行），系统管理员账号及其菜单授权将跳过",
                    SYSTEM_ADMIN_ROLE_CODE);
        }

        // 超级管理员账号（is_admin=1，仅绑定 ADMIN 角色）
        AdminProperties.AdminAccount superAccount =
                requireAccount(adminProperties.superAdmin(), "erp.admin.super-admin");
        SysUser adminUser = sysUserDomainService.getByUsername(superAccount.username());
        boolean superAdminCreated = false;
        if (adminUser == null) {
            adminUser = createUser(superAccount, ADMIN_USER_CODE, "超级管理员", 1);
            superAdminCreated = true;
        }
        // admin 账号幂等确保持有 ADMIN 角色（引导身份与角色身份并存）
        ensureUserRole(adminUser.getId(), adminRole.getId());

        // 系统管理员账号（is_admin=0，仅绑定 SYSTEM_ADMIN 角色，权限集可由超管调整）
        boolean systemAdminCreated = false;
        if (systemAdminRole != null) {
            AdminProperties.AdminAccount systemAccount =
                    requireAccount(adminProperties.systemAdmin(), "erp.admin.system-admin");
            SysUser systemAdminUser = sysUserDomainService.getByUsername(systemAccount.username());
            if (systemAdminUser == null) {
                systemAdminUser = createUser(systemAccount, SYSTEM_ADMIN_USER_CODE, "系统管理员", 0);
                systemAdminCreated = true;
            }
            ensureUserRole(systemAdminUser.getId(), systemAdminRole.getId());
        }

        List<SysMenu> allMenus = new ArrayList<>();
        List<SysMenu> createdMenus = new ArrayList<>();
        ensureTree(0L, menuTree(), allMenus, createdMenus);

        // 全新初始化授予全量菜单；老库只补本次新增菜单
        List<SysMenu> toGrant = superAdminCreated || systemAdminCreated ? allMenus : createdMenus;

        // 角色-菜单授权：ADMIN 必授；SYSTEM_ADMIN 存在时同授
        List<Long> grantRoleIds = systemAdminRole == null
                ? List.of(adminRole.getId())
                : List.of(adminRole.getId(), systemAdminRole.getId());
        List<SysRoleMenu> roleMenus = new ArrayList<>();
        for (SysMenu menu : toGrant) {
            for (Long roleId : grantRoleIds) {
                roleMenus.add(roleConverter.toSysRoleMenu(roleId, menu.getId()));
            }
        }
        if (!roleMenus.isEmpty()) {
            sysRoleMenuService.createBatch(roleMenus);
        }
        log.info("管理员初始化完成 | 超管: {} (已创建: {}) | 系统管理员已创建: {} | 新增菜单数: {} | 补授权菜单数: {}",
                superAccount.username(), superAdminCreated, systemAdminCreated,
                createdMenus.size(), roleMenus.size());
    }

    /**
     * 校验并取回初始账号配置，防止空配置静默落库；沿用默认口令时告警提示。
     */
    private AdminProperties.AdminAccount requireAccount(AdminProperties.AdminAccount account, String configKey) {
        if (account == null || account.isBlank()) {
            throw new IllegalStateException("管理员初始账号配置缺失或不完整，请检查 " + configKey);
        }
        if (DEFAULT_PASSWORD.equals(account.password())) {
            log.warn("账号 {} 仍在使用默认初始口令，生产环境请通过环境变量覆盖 {}", account.username(), configKey + ".password");
        }
        return account;
    }

    /**
     * 幂等确保持有指定角色：无关联时写入（老库 admin 账号补持 ADMIN 角色等场景）。
     */
    private void ensureUserRole(Long userId, Long roleId) {
        if (!sysUserRoleService.listRoleIdsByUserId(userId).contains(roleId)) {
            sysUserRoleService.createForUser(userId, roleId);
        }
    }

    /**
     * 创建超级管理员角色（仅当 seed.sql 未执行时的兜底）。
     */
    private SysRole createAdminRole() {
        SysRole role = new SysRole();
        role.setRoleCode(ADMIN_ROLE_CODE);
        role.setRoleName("超级管理员");
        role.setDataScope(1);
        role.setIsBuiltin(1);
        role.setStatus(1);
        sysRoleDomainService.createRole(role);
        return role;
    }

    /**
     * 创建初始管理员账号：统一 BCrypt 加密口令、状态启用、口令更新时间置当前。
     *
     * @param account  初始账号配置（用户名与明文口令）
     * @param userCode 用户编号
     * @param realName 姓名
     * @param isAdmin  是否超管万能身份（1-是 0-否）
     */
    private SysUser createUser(AdminProperties.AdminAccount account, String userCode, String realName, int isAdmin) {
        SysUser user = new SysUser();
        user.setUserCode(userCode);
        user.setUsername(account.username());
        user.setPassword(passwordEncoder.encode(account.password()));
        user.setRealName(realName);
        user.setGender(0);
        user.setStatus(1);
        user.setIsAdmin(isAdmin);
        user.setLoginFailCount(0);
        user.setPasswordUpdateTime(LocalDateTime.now());
        sysUserDomainService.createUser(user);
        return user;
    }

    /**
     * 系统管理菜单树定义（目录 → 菜单 → 按钮）。
     * <p>权限点必须与各 Controller 的 @PreAuthorize 一一对应（admin 权限取全表 perms 并集，
     * 缺任何一个会导致对应接口 403）；登录日志 perms 为全小写 system:loginlog:list。</p>
     */
    private List<MenuSeed> menuTree() {
        return List.of(
            MenuSeed.dir("系统管理", "/system", "Setting", 1,
                MenuSeed.menu("用户管理", "/system/user", "system/user/index", "system:user:list", "User", 2,
                    MenuSeed.button("用户详情", "system:user:detail", 1),
                    MenuSeed.button("用户新增", "system:user:create", 2),
                    MenuSeed.button("用户修改", "system:user:update", 3),
                    MenuSeed.button("用户删除", "system:user:delete", 4),
                    MenuSeed.button("用户导出", "system:user:export", 5),
                    MenuSeed.button("重置密码", "system:user:resetpwd", 6),
                    MenuSeed.button("分配角色", "system:user:assignrole", 7)),
                MenuSeed.menu("角色管理", "/system/role", "system/role/index", "system:role:list", "Team", 2,
                    MenuSeed.button("角色详情", "system:role:detail", 1),
                    MenuSeed.button("角色新增", "system:role:create", 2),
                    MenuSeed.button("角色修改", "system:role:update", 3),
                    MenuSeed.button("角色删除", "system:role:delete", 4),
                    MenuSeed.button("分配菜单", "system:role:assignmenu", 5),
                    MenuSeed.button("数据权限", "system:role:datascope", 6)),
                MenuSeed.menu("菜单管理", "/system/menu", "system/menu/index", "system:menu:list", "Menu", 3,
                    MenuSeed.button("菜单新增", "system:menu:create", 1),
                    MenuSeed.button("菜单修改", "system:menu:update", 2),
                    MenuSeed.button("菜单删除", "system:menu:delete", 3)),
                MenuSeed.menu("组织管理", "/system/dept", "system/dept/index", "system:dept:list", "Apartment", 4,
                    MenuSeed.button("组织新增", "system:dept:create", 1),
                    MenuSeed.button("组织修改", "system:dept:update", 2),
                    MenuSeed.button("组织删除", "system:dept:delete", 3)),
                MenuSeed.menu("字典管理", "/system/dict", "system/dict/index", "system:dict:list", "Book", 5,
                    MenuSeed.button("字典新增", "system:dict:create", 1),
                    MenuSeed.button("字典修改", "system:dict:update", 2),
                    MenuSeed.button("字典删除", "system:dict:delete", 3)),
                MenuSeed.menu("参数管理", "/system/config", "system/config/index", "system:config:list", "Tool", 6,
                    MenuSeed.button("参数新增", "system:config:create", 1),
                    MenuSeed.button("参数修改", "system:config:update", 2),
                    MenuSeed.button("参数删除", "system:config:delete", 3)),
                MenuSeed.menu("登录日志", "/system/login-log", "system/loginLog/index", "system:loginlog:list", "FileText", 7),
                MenuSeed.menu("审计日志", "/system/audit-log", "system/auditLog/index", "system:auditlog:list", "Scroll", 8,
                    MenuSeed.button("审计详情", "system:auditlog:detail", 1)),
                MenuSeed.menu("附件管理", "/system/attachment", "system/attachment/index", "system:attachment:list", "Paperclip", 9,
                    MenuSeed.button("附件上传", "system:attachment:upload", 1),
                    MenuSeed.button("附件下载", "system:attachment:download", 2),
                    MenuSeed.button("附件删除", "system:attachment:delete", 3)),
                MenuSeed.menu("导入任务", "/system/import-task", "system/importTask/index", "system:import:detail", "UploadCloud", 10),
                MenuSeed.menu("编号序列", "/system/code-sequence", "system/codeSequence/index", "system:sequence:list", "Hash", 11,
                    MenuSeed.button("序列重置", "system:sequence:reset", 1))));
    }

    /**
     * 递归落库菜单树：按名称幂等，收集全部节点与本次新增节点。
     * <p>已存在的菜单若 perms 与种子不一致（如历史大小写偏差），原地修正，保证权限点与代码同步。</p>
     */
    private void ensureTree(Long parentId, List<MenuSeed> seeds, List<SysMenu> allMenus, List<SysMenu> createdMenus) {
        for (MenuSeed seed : seeds) {
            SysMenu menu = sysMenuDomainService.getByMenuName(seed.name());
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
                menu.setIsSystem(1);
                sysMenuDomainService.createMenu(menu);
                createdMenus.add(menu);
            } else {
                // 种子菜单即内置菜单（08 文档 2.2）：老库未标记 is_system 的幂等补标
                sysMenuDomainService.markSystem(menu);
                if (seed.perms() != null && !seed.perms().equals(menu.getPerms())) {
                    sysMenuDomainService.updatePerms(menu.getId(), seed.perms());
                }
            }
            allMenus.add(menu);
            ensureTree(menu.getId(), seed.children(), allMenus, createdMenus);
        }
    }
}
