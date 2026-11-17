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
import me.north30.erp.system.common.util.SiblingSortUtil;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.converter.MenuConverter;
import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuMoveDTO;
import me.north30.erp.system.menu.dto.MenuStatusDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.menu.service.SysMenuApplicationService;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import me.north30.erp.system.menu.vo.MenuCreatedVO;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuStatusVO;
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
        // 校验顺序：格式（类型必填字段）→ 结构（父级存在性 + 父子类型配对）→ 深度 → perms 唯一 → 入库
        SysMenu menu = menuConverter.createSysMenu(dto);
        menu.requireTypeFieldsValid();
        sysMenuDomainService.requireValidParent(dto.parentId(), menu.getMenuType());
        sysMenuDomainService.requireDepthWithinLimit(dto.parentId(), menu.getMenuType());
        sysMenuDomainService.requirePermsAvailable(menu.getPerms(), null);
        menu.applyCreateDefaults();
        menu.clearRouteFieldsIfButton();
        // 未显式指定显示顺序时取同级 max(menuSort)+10（08 文档阶段四排序统一）
        if (dto.menuSort() == null) {
            menu.setMenuSort(sysMenuDomainService.nextMenuSort(menu.getParentId()));
        }
        sysMenuMapper.insert(menu);
        return new MenuCreatedVO(menu.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuUpdatedVO update(Long id, MenuUpdateDTO dto) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        Integer originalType = menu.getMenuType();
        Long originalParentId = menu.getParentId();
        // 先合并 DTO 得到最终 parentId/menuType，所有校验必须使用合并后的最终值
        menuConverter.updateSysMenu(dto, menu);
        // 有子节点时禁止修改菜单类型（避免子节点与新类型配对违规，形成结构倒挂）
        if (!menu.getMenuType().equals(originalType)) {
            Long childCount = sysMenuMapper.selectCount(
                new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, id));
            if (childCount != null && childCount > 0) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                    "菜单存在子节点，不可修改类型，请先处理子节点");
            }
        }
        menu.requireTypeFieldsValid();
        sysMenuDomainService.requireValidParent(menu.getParentId(), menu.getMenuType());
        // 换父级时追加防环 + 子树深度守卫
        if (dto.parentId() != null && !dto.parentId().equals(originalParentId)) {
            sysMenuDomainService.requireNotDescendant(id, menu.getParentId());
            sysMenuDomainService.requireSubtreeDepthWithinLimit(id, menu.getParentId());
        }
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
    public void move(Long id, MenuMoveDTO dto) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        // 同级按 sort ASC, id ASC 一次查出，规范化重排 + 相邻交换，变更行逐一乐观锁落库
        List<SysMenu> siblings = sysMenuDomainService.listSiblings(menu.getParentId());
        List<SysMenu> changed = SiblingSortUtil.moveSibling(siblings, menu,
            "up".equals(dto.direction()), SysMenu::getId, SysMenu::getMenuSort, SysMenu::setMenuSort);
        for (SysMenu row : changed) {
            UpdateGuardUtil.checkOptimisticLockConflict(sysMenuMapper.updateById(row));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MenuStatusVO changeStatus(Long id, MenuStatusDTO dto) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        // 停用前校验子树不含内置菜单（18039）；状态值域（10001）与自身内置保护收敛于实体 changeStatus
        if (dto.status() == 0) {
            sysMenuDomainService.requireSubtreeNotSystem(id);
        }
        menu.changeStatus(dto.status());
        SysMenu patch = new SysMenu();
        patch.setId(id);
        patch.setStatus(menu.getStatus());
        patch.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysMenuMapper.updateById(patch));
        return new MenuStatusVO(menu.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysMenu menu = sysMenuDomainService.requireMenu(id);
        // 内置菜单不可删除（18039）
        if (Integer.valueOf(1).equals(menu.getIsSystem())) {
            throw new BusinessException(SystemErrorCode.SYSTEM_MENU_PROTECTED,
                "内置菜单 " + menu.getMenuName() + " 不可删除");
        }
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
