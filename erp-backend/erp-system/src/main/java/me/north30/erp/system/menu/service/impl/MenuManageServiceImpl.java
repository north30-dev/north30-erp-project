package me.north30.erp.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.role.mapper.SysRoleMenuMapper;
import me.north30.erp.system.menu.service.MenuManageService;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.vo.MenuCreatedVO;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuUpdatedVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单与权限点管理服务实现（接口文档 5.3）。
 * <p>树查询一次性查出全部后内存组树（禁止 N+1）；perms 唯一应用层校验；
 * 写操作显式事务 + 乐观锁。</p>
 */
@Service
@RequiredArgsConstructor
public class MenuManageServiceImpl implements MenuManageService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 菜单类型：目录 */
    private static final int TYPE_DIR = 1;
    /** 菜单类型：菜单 */
    private static final int TYPE_MENU = 2;
    /** 菜单类型：按钮 */
    private static final int TYPE_BUTTON = 3;

    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    @Transactional(readOnly = true)
    public List<MenuNodeVO> tree(MenuTreeQueryDTO query) {
        // 单次查询全部菜单后内存组树，禁止循环内查库
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
            .like(hasText(query.menuName()), SysMenu::getMenuName, query.menuName())
            .like(hasText(query.perms()), SysMenu::getPerms, query.perms())
            .eq(query.status() != null, SysMenu::getStatus, query.status()));
        List<MenuNodeVO> nodes = menus.stream().map(this::toNode).toList();
        Map<Long, List<MenuNodeVO>> childrenMap = nodes.stream()
            .filter(node -> node.getParentId() != null && node.getParentId() != ROOT_PARENT_ID)
            .collect(Collectors.groupingBy(MenuNodeVO::getParentId));
        for (MenuNodeVO node : nodes) {
            List<MenuNodeVO> children = childrenMap.get(node.getId());
            if (children != null) {
                children.sort(menuSortComparator());
                node.setChildren(children);
            }
        }
        return nodes.stream()
            .filter(node -> node.getParentId() == null || node.getParentId() == ROOT_PARENT_ID)
            .sorted(menuSortComparator())
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuCreatedVO create(MenuCreateDTO dto) {
        validateMenuType(dto.menuType());
        // 按钮下不可再建子节点（最多三级：目录 → 菜单 → 按钮）
        if (dto.parentId() != null && dto.parentId() != ROOT_PARENT_ID) {
            SysMenu parent = sysMenuMapper.selectById(dto.parentId());
            if (parent == null) {
                throw new BusinessException(RoleMenuErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除");
            }
            if (parent.getMenuType() != null && parent.getMenuType() == TYPE_BUTTON) {
                throw new BusinessException(RoleMenuErrorCode.MENU_LEVEL_EXCEEDED);
            }
        }
        validateTypeFields(dto.menuType(), dto.path(), dto.component(), dto.perms());
        checkPermsUnique(dto.perms(), null);
        SysMenu menu = new SysMenu();
        menu.setMenuName(dto.menuName());
        menu.setParentId(dto.parentId());
        menu.setMenuType(dto.menuType());
        menu.setPath(dto.path());
        menu.setComponent(dto.component());
        menu.setPerms(dto.perms());
        menu.setIcon(dto.icon());
        menu.setMenuSort(dto.menuSort() == null ? 0 : dto.menuSort());
        menu.setVisible(dto.visible() == null ? 1 : dto.visible());
        menu.setStatus(dto.status() == null ? 1 : dto.status());
        menu.setRemark(dto.remark());
        // 按钮不承载路由与组件信息
        if (menu.getMenuType() == TYPE_BUTTON) {
            menu.setPath(null);
            menu.setComponent(null);
        }
        sysMenuMapper.insert(menu);
        return new MenuCreatedVO(menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuUpdatedVO update(Long id, MenuUpdateDTO dto) {
        SysMenu menu = requireMenu(id);
        if (dto.menuType() != null) {
            validateMenuType(dto.menuType());
            menu.setMenuType(dto.menuType());
        }
        if (dto.parentId() != null) {
            validateParent(id, dto.parentId());
            menu.setParentId(dto.parentId());
        }
        if (dto.menuName() != null) {
            menu.setMenuName(dto.menuName());
        }
        if (dto.path() != null) {
            menu.setPath(dto.path());
        }
        if (dto.component() != null) {
            menu.setComponent(dto.component());
        }
        if (dto.perms() != null) {
            menu.setPerms(dto.perms());
        }
        if (dto.icon() != null) {
            menu.setIcon(dto.icon());
        }
        if (dto.menuSort() != null) {
            menu.setMenuSort(dto.menuSort());
        }
        if (dto.visible() != null) {
            menu.setVisible(dto.visible());
        }
        if (dto.status() != null) {
            menu.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            menu.setRemark(dto.remark());
        }
        validateTypeFields(menu.getMenuType(), menu.getPath(), menu.getComponent(), menu.getPerms());
        checkPermsUnique(menu.getPerms(), id);
        if (menu.getMenuType() == TYPE_BUTTON) {
            menu.setPath(null);
            menu.setComponent(null);
        }
        // 乐观锁按客户端传入 version 校验
        menu.setVersion(dto.version());
        int rows = sysMenuMapper.updateById(menu);
        if (rows == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        SysMenu latest = sysMenuMapper.selectById(id);
        return new MenuUpdatedVO(latest != null && latest.getUpdateTime() != null
            ? latest.getUpdateTime().format(DATETIME_FORMATTER)
            : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysMenu menu = requireMenu(id);
        Long childCount = sysMenuMapper.selectCount(
            new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(RoleMenuErrorCode.MENU_HAS_CHILDREN,
                "菜单 " + menu.getMenuName() + " 存在子节点，不可删除");
        }
        Long refCount = sysRoleMenuMapper.selectCount(
            new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getMenuId, id));
        if (refCount != null && refCount > 0) {
            throw new BusinessException(RoleMenuErrorCode.MENU_REFERENCED_BY_ROLE,
                "菜单 " + menu.getMenuName() + " 已被角色引用，不可删除");
        }
        sysMenuMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    private MenuNodeVO toNode(SysMenu menu) {
        MenuNodeVO node = new MenuNodeVO();
        node.setId(menu.getId());
        node.setMenuName(menu.getMenuName());
        node.setParentId(menu.getParentId());
        node.setMenuType(menu.getMenuType());
        node.setPath(menu.getPath());
        node.setComponent(menu.getComponent());
        node.setPerms(menu.getPerms());
        node.setIcon(menu.getIcon());
        node.setMenuSort(menu.getMenuSort());
        node.setVisible(menu.getVisible());
        node.setStatus(menu.getStatus());
        return node;
    }

    private static Comparator<MenuNodeVO> menuSortComparator() {
        return Comparator.comparing(MenuNodeVO::getMenuSort, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private SysMenu requireMenu(Long id) {
        SysMenu menu = sysMenuMapper.selectById(id);
        if (menu == null) {
            throw new BusinessException(RoleMenuErrorCode.MENU_NOT_FOUND);
        }
        return menu;
    }

    /**
     * 菜单类型合法性校验（1-目录 2-菜单 3-按钮）。
     */
    private void validateMenuType(Integer menuType) {
        if (menuType == null || menuType < TYPE_DIR || menuType > TYPE_BUTTON) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "菜单类型非法：" + menuType);
        }
    }

    /**
     * 按类型校验路由/组件/权限标识：目录与菜单必填 path（菜单还需 component）；
     * 按钮必填 perms 且唯一。
     */
    private void validateTypeFields(Integer menuType, String path, String component, String perms) {
        if (menuType == TYPE_DIR && !hasText(path)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "目录必须配置路由地址");
        }
        if (menuType == TYPE_MENU && (!hasText(path) || !hasText(component))) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "菜单必须配置路由地址与前端组件路径");
        }
        if (menuType == TYPE_BUTTON && !hasText(perms)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "按钮必须配置权限标识");
        }
    }

    /**
     * perms 唯一校验（应用层保证，excludeId 用于修改场景排除自身）。
     */
    private void checkPermsUnique(String perms, Long excludeId) {
        if (!hasText(perms)) {
            return;
        }
        Long count = sysMenuMapper.selectCount(new LambdaQueryWrapper<SysMenu>()
            .eq(SysMenu::getPerms, perms)
            .ne(excludeId != null, SysMenu::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(RoleMenuErrorCode.MENU_PERMS_DUPLICATED,
                "权限标识 " + perms + " 已存在");
        }
    }

    /**
     * 父级合法性校验：不得为按钮、不得指向自身或自身下级（防环，逐级向上回溯）。
     */
    private void validateParent(Long selfId, Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        if (parentId.equals(selfId)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身");
        }
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(RoleMenuErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除");
        }
        if (parent.getMenuType() != null && parent.getMenuType() == TYPE_BUTTON) {
            throw new BusinessException(RoleMenuErrorCode.MENU_LEVEL_EXCEEDED);
        }
        // 防环：从新父级沿父链向上回溯，若回到自身则说明父级位于自身下级
        Long cursor = parentId;
        int guard = 0;
        while (cursor != null && cursor != ROOT_PARENT_ID && guard++ < 10) {
            if (cursor.equals(selfId)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身或自身下级");
            }
            SysMenu current = sysMenuMapper.selectById(cursor);
            if (current == null) {
                break;
            }
            cursor = current.getParentId();
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
