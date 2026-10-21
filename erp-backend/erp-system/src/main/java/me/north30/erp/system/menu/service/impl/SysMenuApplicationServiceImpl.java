package me.north30.erp.system.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.common.exception.BusinessException;
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
        // 父级合法性（存在性 + 非按钮 + 防环）与 perms 唯一守卫收敛进领域服务；
        // 按钮下禁建子仅在 requireValidParent 判断一次
        if (dto.parentId() != null && dto.parentId() != ROOT_PARENT_ID) {
            sysMenuDomainService.requireValidParent(null, dto.parentId());
        }
        SysMenu menu = menuConverter.createSysMenu(dto);
        menu.requireTypeFieldsValid();
        sysMenuDomainService.requirePermsAvailable(menu.getPerms(), null);
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
            sysMenuDomainService.requireValidParent(id, dto.parentId());
        }
        // null 字段跳过（部分更新语义），按钮字段清空与乐观锁 version 由下方逻辑处理
        menuConverter.updateSysMenu(dto, menu);
        menu.requireTypeFieldsValid();
        sysMenuDomainService.requirePermsAvailable(menu.getPerms(), id);
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
}
