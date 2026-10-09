package me.north30.erp.system.auth.strategy;

import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.role.service.SysRoleService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.menu.service.SysMenuService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import lombok.RequiredArgsConstructor;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 访问装配策略：认证链的角色/权限点/数据范围/菜单祖先链装配。
 * <p>纯编排装配，依赖取数 Service 完成批量查询后在内存组装，杜绝循环查库（N+1）。</p>
 */
@Component
@RequiredArgsConstructor
public class AccessAssembleStrategy {

    /** 数据范围档位按"最宽"排序：1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private static final List<Integer> DATA_SCOPE_ORDER = List.of(1, 2, 3, 4, 5, 6, 9);

    private final SysUserRoleService sysUserRoleService;
    private final SysRoleService sysRoleService;
    private final SysRoleMenuService sysRoleMenuService;
    private final SysMenuService sysMenuService;

    /**
     * 查询用户启用角色集合（两次批量查询，无循环查库）。
     */
    public List<SysRole> loadEnabledRoles(Long userId) {
        List<Long> roleIds = sysUserRoleService.listByUserId(userId).stream()
            .map(SysUserRole::getRoleId)
            .toList();
        return sysRoleService.listByIds(roleIds).stream()
            .filter(role -> role.getStatus() != null && role.getStatus() == 1)
            .toList();
    }

    /**
     * 加载权限点集合：admin 全量；普通用户按角色并集（角色-菜单关联与菜单权限标识各一次批量查询）。
     */
    public Set<String> loadPerms(boolean isAdmin, List<SysRole> roles) {
        if (isAdmin) {
            return new LinkedHashSet<>(sysMenuService.listAllPerms());
        }
        if (CollectionUtils.isEmpty(roles)) {
            return new LinkedHashSet<>();
        }
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        List<SysRoleMenu> roleMenus = sysRoleMenuService.listByRoleIds(roleIds);
        if (CollectionUtils.isEmpty(roleMenus)) {
            return new LinkedHashSet<>();
        }
        Set<Long> menuIds = new HashSet<>(roleMenus.stream().map(SysRoleMenu::getMenuId).toList());
        return sysMenuService.listEnabledByIds(menuIds).stream()
            .map(SysMenu::getPerms)
            .filter(perms -> perms != null && !perms.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 多角色取最宽数据范围档位（1-全部 > 2 > 3 > 4 > 5 > 6 > 9-自定义；无角色按"仅本人"）。
     */
    public Integer resolveWidestDataScope(List<SysRole> roles) {
        if (roles.isEmpty()) {
            return 6;
        }
        Set<Integer> scopes = roles.stream()
            .map(SysRole::getDataScope)
            .filter(scope -> scope != null)
            .collect(Collectors.toSet());
        return DATA_SCOPE_ORDER.stream()
            .filter(scopes::contains)
            .findFirst()
            .orElse(9);
    }

    /**
     * 内存补齐菜单父级链：从角色勾选菜单出发，沿启用菜单向上收集祖先 ID。
     * <p>父级不在启用集合（停用/不存在）时停止向上追溯，与逐层查库版本语义一致。</p>
     */
    public Set<Long> expandWithAncestors(Set<Long> menuIds, Map<Long, SysMenu> enabledById) {
        Set<Long> result = new HashSet<>(menuIds);
        for (Long menuId : menuIds) {
            SysMenu menu = enabledById.get(menuId);
            while (menu != null) {
                Long parentId = menu.getParentId();
                if (parentId == null || parentId == 0L || !result.add(parentId)) {
                    break;
                }
                menu = enabledById.get(parentId);
            }
        }
        return result;
    }
}
