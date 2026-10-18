package me.north30.erp.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.MenuTreeUtil;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.converter.MenuConverter;
import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.menu.service.SysMenuApplicationService;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import me.north30.erp.system.menu.vo.MenuCreatedVO;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuUpdatedVO;
import me.north30.erp.system.role.service.SysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 菜单与权限点管理服务实现（接口文档 5.3）。
 * <p>树查询一次性查出全部后内存组树（{@link MenuTreeUtil}，禁止 N+1）；perms 唯一应用层校验；
 * 菜单类型与按类型必填字段校验由 {@link SysMenu#requireTypeFieldsValid()} 承载；写操作显式事务 + 乐观锁。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuApplicationServiceImpl implements SysMenuApplicationService {

    /** 顶级菜单父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private final SysMenuMapper sysMenuMapper;
    private final SysMenuDomainService sysMenuDomainService;
    private final SysRoleMenuService sysRoleMenuService;
    private final MenuTreeUtil menuTreeUtil;
    private final MenuConverter menuConverter;

    @Override
    @Transactional(readOnly = true)
    public List<MenuNodeVO> tree(MenuTreeQueryDTO query) {
        // 单次查询全部菜单后内存组树，禁止循环内查库
        List<SysMenu> menus = sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
            .like(StringUtils.hasText(query.menuName()), SysMenu::getMenuName, query.menuName())
            .like(StringUtils.hasText(query.perms()), SysMenu::getPerms, query.perms())
            .eq(query.status() != null, SysMenu::getStatus, query.status()));
        List<MenuNodeVO> nodes = menus.stream().map(menuConverter::toMenuNodeVO).toList();
        return menuTreeUtil.buildNodeTree(nodes);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuCreatedVO create(MenuCreateDTO dto) {
        // 按钮下不可再建子节点（最多三级：目录 → 菜单 → 按钮）
        if (dto.parentId() != null && dto.parentId() != ROOT_PARENT_ID) {
            SysMenu parent = requireParent(dto.parentId());
            if (parent.getMenuType() != null && parent.getMenuType() == SysMenu.TYPE_BUTTON) {
                throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
            }
        }
        SysMenu menu = menuConverter.createSysMenu(dto);
        menu.requireTypeFieldsValid();
        checkPermsUnique(menu.getPerms(), null);
        menu.applyCreateDefaults();
        menu.clearRouteFieldsIfButton();
        sysMenuMapper.insert(menu);
        return new MenuCreatedVO(menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuUpdatedVO update(Long id, MenuUpdateDTO dto) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        if (dto.parentId() != null) {
            validateParent(id, dto.parentId());
        }
        // null 字段跳过（部分更新语义），按钮字段清空与乐观锁 version 由下方逻辑处理
        menuConverter.updateSysMenu(dto, menu);
        menu.requireTypeFieldsValid();
        checkPermsUnique(menu.getPerms(), id);
        menu.clearRouteFieldsIfButton();
        // 乐观锁按客户端传入 version 校验
        menu.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysMenuMapper.updateById(menu));
        SysMenu latest = sysMenuMapper.selectById(id);
        return new MenuUpdatedVO(latest != null && latest.getUpdateTime() != null
            ? DateTimeFormatUtil.format(latest.getUpdateTime())
            : null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        Long childCount = sysMenuMapper.selectCount(
            new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(SystemErrorCode.MENU_HAS_CHILDREN,
                "菜单 " + menu.getMenuName() + " 存在子节点，不可删除");
        }
        if (sysRoleMenuService.existsByMenuId(id)) {
            throw new BusinessException(SystemErrorCode.MENU_REFERENCED_BY_ROLE,
                "菜单 " + menu.getMenuName() + " 已被角色引用，不可删除");
        }
        sysMenuMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    /**
     * perms 唯一校验（应用层保证，excludeId 用于修改场景排除自身）。
     */
    private void checkPermsUnique(String perms, Long excludeId) {
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

    /**
     * 父级菜单必须存在，否则抛"上级菜单不存在或已删除"。
     */
    private SysMenu requireParent(Long parentId) {
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除");
        }
        return parent;
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
        SysMenu parent = requireParent(parentId);
        if (parent.getMenuType() != null && parent.getMenuType() == SysMenu.TYPE_BUTTON) {
            throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
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
}
