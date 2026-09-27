package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.MenuCreateDTO;
import me.north30.erp.system.core.dto.MenuTreeQueryDTO;
import me.north30.erp.system.core.dto.MenuUpdateDTO;
import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.mapper.SysMenuMapper;
import me.north30.erp.system.core.mapper.SysRoleMenuMapper;
import me.north30.erp.system.core.vo.MenuNodeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MenuManageServiceImpl 单元测试：
 * 覆盖树构建单次查询（无 N+1）、删除含子节点/被角色引用守卫、类型校验
 * （按钮必填 perms、按钮下禁止建子节点、perms 唯一、防环）。
 * 全部 Mapper 为 Mock，不依赖数据库。
 */
@ExtendWith(MockitoExtension.class)
class MenuManageServiceImplTest {

    private static final Long MENU_ID = 200L;

    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    private MenuManageServiceImpl menuManageService;

    @BeforeEach
    void setUp() {
        menuManageService = new MenuManageServiceImpl(sysMenuMapper, sysRoleMenuMapper);
    }

    private SysMenu menu(Long id, Long parentId, Integer menuType, String perms, int sort) {
        SysMenu menu = new SysMenu();
        menu.setId(id);
        menu.setMenuName("菜单" + id);
        menu.setParentId(parentId);
        menu.setMenuType(menuType);
        menu.setPerms(perms);
        menu.setPath(menuType == 3 ? null : "/path" + id);
        menu.setComponent(menuType == 3 ? null : "component" + id);
        menu.setMenuSort(sort);
        menu.setVisible(1);
        menu.setStatus(1);
        return menu;
    }

    @Test
    @DisplayName("菜单树：单次查询后内存组树，目录→菜单→按钮三级挂载")
    void tree_buildsWithSingleQuery() {
        SysMenu dir = menu(1L, 0L, 1, null, 1);
        SysMenu page = menu(2L, 1L, 2, "system:role:list", 1);
        SysMenu button = menu(3L, 2L, 3, "system:role:create", 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(button, dir, page));

        List<MenuNodeVO> tree = menuManageService.tree(new MenuTreeQueryDTO(null, null, null));

        // 单次查询，无 N+1
        verify(sysMenuMapper, times(1)).selectList(any());
        assertEquals(1, tree.size());
        MenuNodeVO dirNode = tree.get(0);
        assertEquals(1L, dirNode.getId());
        assertEquals(1, dirNode.getMenuType());
        assertEquals(1, dirNode.getChildren().size());
        MenuNodeVO pageNode = dirNode.getChildren().get(0);
        assertEquals(2L, pageNode.getId());
        assertEquals("system:role:list", pageNode.getPerms());
        assertEquals(1, pageNode.getChildren().size());
        assertEquals(3L, pageNode.getChildren().get(0).getId());
        assertEquals("system:role:create", pageNode.getChildren().get(0).getPerms());
    }

    @Test
    @DisplayName("菜单树：同级按 menuSort 升序排列")
    void tree_sortsSiblingsByMenuSort() {
        SysMenu dir = menu(1L, 0L, 1, null, 1);
        SysMenu second = menu(20L, 1L, 2, null, 2);
        SysMenu first = menu(10L, 1L, 2, null, 1);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(dir, second, first));

        List<MenuNodeVO> tree = menuManageService.tree(new MenuTreeQueryDTO(null, null, null));

        assertEquals(10L, tree.get(0).getChildren().get(0).getId());
        assertEquals(20L, tree.get(0).getChildren().get(1).getId());
    }

    @Test
    @DisplayName("删除菜单：存在子节点时抛 18020")
    void delete_menuWithChildren_rejected() {
        when(sysMenuMapper.selectById(MENU_ID)).thenReturn(menu(MENU_ID, 0L, 1, null, 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> menuManageService.delete(MENU_ID));

        assertEquals(18020, exception.getCode());
        assertTrue(exception.getMessage().contains("子节点"));
        verify(sysMenuMapper, never()).deleteById(MENU_ID);
    }

    @Test
    @DisplayName("删除菜单：已被角色引用时抛 18021")
    void delete_menuReferencedByRole_rejected() {
        when(sysMenuMapper.selectById(MENU_ID)).thenReturn(menu(MENU_ID, 0L, 1, null, 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMenuMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> menuManageService.delete(MENU_ID));

        assertEquals(18021, exception.getCode());
        verify(sysMenuMapper, never()).deleteById(MENU_ID);
    }

    @Test
    @DisplayName("删除菜单：叶子且无引用时逻辑删除")
    void delete_leafMenu_success() {
        when(sysMenuMapper.selectById(MENU_ID)).thenReturn(menu(MENU_ID, 0L, 1, null, 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMenuMapper.selectCount(any())).thenReturn(0L);

        var vo = menuManageService.delete(MENU_ID);

        assertEquals(MENU_ID, vo.id());
        assertEquals(1, vo.isDeleted());
        verify(sysMenuMapper).deleteById(MENU_ID);
    }

    @Test
    @DisplayName("新增菜单：按钮未配置权限标识抛 10001")
    void create_buttonWithoutPerms_rejected() {
        when(sysMenuMapper.selectById(2L)).thenReturn(menu(2L, 1L, 2, "system:role:list", 1));

        BusinessException exception = assertThrows(BusinessException.class, () ->
            menuManageService.create(new MenuCreateDTO("用户新增", 2L, 3, null, null, null,
                null, 1, null, null, null)));

        assertEquals(10001, exception.getCode());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：按钮下再建子节点抛 18023")
    void create_childUnderButton_rejected() {
        SysMenu button = menu(9L, 2L, 3, "system:role:create", 1);
        when(sysMenuMapper.selectById(9L)).thenReturn(button);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            menuManageService.create(new MenuCreateDTO("四级菜单", 9L, 1, "/x", "x/index", null,
                null, 1, null, null, null)));

        assertEquals(18023, exception.getCode());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：权限标识重复抛 18022")
    void create_duplicatePerms_rejected() {
        when(sysMenuMapper.selectById(2L)).thenReturn(menu(2L, 1L, 2, "system:role:list", 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            menuManageService.create(new MenuCreateDTO("用户新增", 2L, 3, null, null,
                "system:user:create", null, 1, null, null, null)));

        assertEquals(18022, exception.getCode());
        assertTrue(exception.getMessage().contains("system:user:create"));
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：菜单缺组件路径抛 10001")
    void create_menuWithoutComponent_rejected() {
        when(sysMenuMapper.selectById(1L)).thenReturn(menu(1L, 0L, 1, null, 1));

        BusinessException exception = assertThrows(BusinessException.class, () ->
            menuManageService.create(new MenuCreateDTO("角色管理", 1L, 2, "/system/role", null, null,
                null, 1, null, null, null)));

        assertEquals(10001, exception.getCode());
        verify(sysMenuMapper, never()).insert(any(SysMenu.class));
    }

    @Test
    @DisplayName("新增菜单：合法按钮清除路由/组件字段后写入")
    void create_button_success_clearsRouteFields() {
        when(sysMenuMapper.selectById(2L)).thenReturn(menu(2L, 1L, 2, "system:role:list", 1));
        when(sysMenuMapper.selectCount(any())).thenReturn(0L);

        menuManageService.create(new MenuCreateDTO("角色新增", 2L, 3, "/accidental", "accidental/index",
            "system:role:create", null, 1, null, null, null));

        ArgumentCaptor<SysMenu> captor = ArgumentCaptor.forClass(SysMenu.class);
        verify(sysMenuMapper).insert(captor.capture());
        SysMenu inserted = captor.getValue();
        assertEquals(3, inserted.getMenuType());
        assertEquals(2L, inserted.getParentId());
        assertNull(inserted.getPath());
        assertNull(inserted.getComponent());
        assertEquals(1, inserted.getStatus());
    }

    @Test
    @DisplayName("修改菜单：父级指向自身抛 10001")
    void update_parentToSelf_rejected() {
        when(sysMenuMapper.selectById(MENU_ID)).thenReturn(menu(MENU_ID, 0L, 1, null, 1));

        BusinessException exception = assertThrows(BusinessException.class, () ->
            menuManageService.update(MENU_ID, new MenuUpdateDTO(null, MENU_ID, null, null, null,
                null, null, null, null, null, null, 0)));

        assertEquals(10001, exception.getCode());
        verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
    }
}
