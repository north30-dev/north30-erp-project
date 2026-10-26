package me.north30.erp.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
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

    @Override
    @Transactional(readOnly = true)
    public List<String> listAllPerms() {
        return listEnabled().stream()
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
    public void requireValidParent(Long selfId, Long parentId) {
        // 顶级（0）或未指定父级直接放行
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return;
        }
        // 防止指向自身或自身下级
        if (parentId.equals(selfId)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身");
        }
        SysMenu parent = sysMenuMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除");
        }
        // 按钮下禁建子（最多三级：目录 → 菜单 → 按钮），此判断仅在此处收敛一次
        if (parent.getMenuType() != null && parent.getMenuType() == SysMenu.TYPE_BUTTON) {
            throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
        } 
        // 按钮只能再菜单下创建
        // if (){

        // }

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
