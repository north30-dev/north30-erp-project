package me.north30.erp.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 菜单与权限点服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysMenuDomainServiceImpl implements SysMenuDomainService {

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private final SysMenuMapper sysMenuMapper;

    @Override
    @Transactional(readOnly = true)
    public SysMenu getByMenuName(String menuName) {
        return sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getMenuName, menuName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listEnabled() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listSiblings(Long parentId) {
        long pid = parentId == null ? ROOT_PARENT_ID : parentId;
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getParentId, pid)
            .orderByAsc(SysMenu::getMenuSort)
            .orderByAsc(SysMenu::getId));
    }

    @Override
    @Transactional(readOnly = true)
    public int nextMenuSort(Long parentId) {
        return listSiblings(parentId).stream()
            .map(SysMenu::getMenuSort)
            .filter(Objects::nonNull)
            .max(Integer::compareTo)
            .orElse(0) + 10;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listAll() {
        return sysMenuMapper.selectList(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listEnabledByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
            .in(SysMenu::getId, ids)
            .eq(SysMenu::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return sysMenuMapper.selectByIds(ids);
    }

    /**
     * 查询全量权限点标识（超管全量权限用，不限菜单状态——停用菜单对超管无感知）。
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> listAllPerms() {
        return sysMenuMapper.selectList(null).stream()
            .map(SysMenu::getPerms)
            .filter(perms -> perms != null && !perms.isBlank())
            .distinct()
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createMenu(SysMenu menu) {
        return sysMenuMapper.insert(menu) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePerms(Long menuId, String perms) {
        SysMenu menu = sysMenuMapper.selectById(menuId);
        if (menu == null) {
            return;
        }
        menu.setPerms(perms);
        sysMenuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markSystem(SysMenu menu) {
        if (Integer.valueOf(1).equals(menu.getIsSystem())) {
            return;
        }
        menu.setIsSystem(1);
        sysMenuMapper.updateById(menu);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireSubtreeNotSystem(Long menuId) {
        Map<Long, SysMenu> menuMap = loadMenuMap();
        if (containsSystem(menuId, menuMap)) {
            throw new BusinessException(SystemErrorCode.SYSTEM_MENU_PROTECTED,
                "目标菜单及其子树包含内置菜单，不可停用");
        }
    }

    /** 递归判定节点及其子孙是否含内置菜单。 */
    private boolean containsSystem(Long menuId, Map<Long, SysMenu> menuMap) {
        SysMenu menu = menuMap.get(menuId);
        if (menu == null) {
            return false;
        }
        if (Integer.valueOf(1).equals(menu.getIsSystem())) {
            return true;
        }
        for (SysMenu child : menuMap.values()) {
            if (menuId.equals(child.getParentId()) && containsSystem(child.getId(), menuMap)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int resetSystemMenus() {
        return sysMenuMapper.update(null, new LambdaUpdateWrapper<SysMenu>()
            .eq(SysMenu::getIsSystem, 1)
            .eq(SysMenu::getStatus, 0)
            .set(SysMenu::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(SystemErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    @Override
    @Transactional(readOnly = true)
    public void requirePermsAvailable(String perms, Long excludeId) {
        // perms 空白（目录无权限点）不参与唯一性约束
        if (!StringUtils.hasText(perms)) {
            return;
        }
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getPerms, perms)
            .ne(excludeId != null, SysMenu::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(SystemErrorCode.MENU_PERMS_DUPLICATED,
                "权限标识 " + perms + " 已存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireValidParent(Long parentId, Integer childType) {
        // 顶级（0）：仅校验子类型非按钮（requireValidChildType 内收敛），不查库
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            SysMenu.requireValidChildType(null, childType);
            return;
        }
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除");
        }
        // 父子类型配对：按钮下禁建子（18023），其余配对违规抛 10001
        SysMenu.requireValidChildType(parent.getMenuType(), childType);
    }

    @Override
    @Transactional(readOnly = true)
    public void requireNotDescendant(Long selfId, Long newParentId) {
        // 防环：从新父级沿父链向上回溯，若回到自身则说明父级位于自身下级
        Map<Long, SysMenu> menuMap = loadMenuMap();
        Long cursor = newParentId;
        while (cursor != null && cursor != ROOT_PARENT_ID) {
            if (cursor.equals(selfId)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身或自身下级");
            }
            SysMenu current = menuMap.get(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDepthWithinLimit(Long parentId, Integer childType) {
        // 顶级子节点深度恒为 1，直接放行不查库
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        Map<Long, SysMenu> menuMap = loadMenuMap();
        int parentDepth = depthOf(parentId, menuMap);
        // 超限抛 18023（父级深度 + 1 ≤ 最大深度 4）
        if (parentDepth + 1 > SysMenu.MAX_DEPTH) {
            throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireSubtreeDepthWithinLimit(Long selfId, Long newParentId) {
        Map<Long, SysMenu> menuMap = loadMenuMap();
        int newParentDepth = newParentId == null || newParentId == ROOT_PARENT_ID ? 0 : depthOf(newParentId, menuMap);
        int subtreeHeight = subtreeHeightOf(selfId, menuMap);
        // 换父级后整体高度 = 新父级深度 + 1 + 子树高度 − 1，超限抛 18023
        if (newParentDepth + subtreeHeight > SysMenu.MAX_DEPTH) {
            throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
        }
    }

    /** 一次全量查询建 id → 实体映射（深度/防环计算复用，禁止循环内查库）。 */
    private Map<Long, SysMenu> loadMenuMap() {
        return sysMenuMapper.selectList(null).stream()
            .collect(Collectors.toMap(BaseEntity::getId, Function.identity()));
    }

    /** 沿父链向上回溯计算节点深度（顶级为 1）；父链断裂时按已回溯层数返回。 */
    private int depthOf(Long id, Map<Long, SysMenu> menuMap) {
        int depth = 0;
        Long cursor = id;
        while (cursor != null && cursor != ROOT_PARENT_ID) {
            depth++;
            SysMenu current = menuMap.get(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
        }
        return depth;
    }

    /** 递归计算以 id 为根的子树高度（自身算 1 层）。 */
    private int subtreeHeightOf(Long id, Map<Long, SysMenu> menuMap) {
        int maxChild = 0;
        for (SysMenu menu : menuMap.values()) {
            if (id.equals(menu.getParentId())) {
                maxChild = Math.max(maxChild, subtreeHeightOf(menu.getId(), menuMap));
            }
        }
        return maxChild + 1;
    }
}
