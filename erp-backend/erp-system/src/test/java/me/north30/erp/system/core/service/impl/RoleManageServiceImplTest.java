package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.RoleAssignMenuDTO;
import me.north30.erp.system.core.dto.RoleCreateDTO;
import me.north30.erp.system.core.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.core.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.core.dto.RoleUpdateDTO;
import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.entity.SysRole;
import me.north30.erp.system.core.entity.SysRoleDataScope;
import me.north30.erp.system.core.entity.SysRoleMenu;
import me.north30.erp.system.core.entity.SysUserRole;
import me.north30.erp.system.core.mapper.SysMenuMapper;
import me.north30.erp.system.core.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.core.mapper.SysRoleMapper;
import me.north30.erp.system.core.mapper.SysRoleMenuMapper;
import me.north30.erp.system.core.mapper.SysUserRoleMapper;
import me.north30.erp.system.core.vo.RoleMenuAssignedVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
 * RoleManageServiceImpl 单元测试：
 * 覆盖角色编码重复、内置/已分配用户角色删除守卫、分配菜单全删全插、
 * 数据范围全量写入与自定义范围校验、乐观锁冲突。
 * 全部 Mapper 为 Mock，不依赖数据库。
 */
@ExtendWith(MockitoExtension.class)
class RoleManageServiceImplTest {

    private static final Long ROLE_ID = 100L;

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper 解析实体 Lambda 需要元数据缓存（无 Spring 容器时手工初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysMenu.class);
        TableInfoHelper.initTableInfo(assistant, SysRoleDataScope.class);
    }

    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;
    @Mock
    private SysMenuMapper sysMenuMapper;
    @Mock
    private SysRoleDataScopeMapper sysRoleDataScopeMapper;

    private RoleManageServiceImpl roleManageService;

    @BeforeEach
    void setUp() {
        roleManageService = new RoleManageServiceImpl(sysRoleMapper, sysUserRoleMapper,
            sysRoleMenuMapper, sysMenuMapper, sysRoleDataScopeMapper);
    }

    private SysRole normalRole() {
        SysRole role = new SysRole();
        role.setId(ROLE_ID);
        role.setRoleCode("sales_manager");
        role.setRoleName("销售经理");
        role.setRoleSort(1);
        role.setDataScope(2);
        role.setIsBuiltin(0);
        role.setStatus(1);
        return role;
    }

    @Test
    @DisplayName("新增角色：编码已存在时抛 18015")
    void create_duplicateCode_rejected() {
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            roleManageService.create(new RoleCreateDTO("admin", "超级管理员", null, null, null, null)));

        assertEquals(18015, exception.getCode());
        assertTrue(exception.getMessage().contains("admin"));
        verify(sysRoleMapper, never()).insert(any(SysRole.class));
    }

    @Test
    @DisplayName("新增角色：合法编码写入并回填默认值")
    void create_success_insertsWithDefaults() {
        when(sysRoleMapper.selectCount(any())).thenReturn(0L);

        roleManageService.create(new RoleCreateDTO("warehouse_keeper", "仓管员", null, null, null, null));

        ArgumentCaptor<SysRole> captor = ArgumentCaptor.forClass(SysRole.class);
        verify(sysRoleMapper).insert(captor.capture());
        SysRole inserted = captor.getValue();
        assertEquals("warehouse_keeper", inserted.getRoleCode());
        assertEquals(0, inserted.getRoleSort());
        assertEquals(1, inserted.getDataScope());
        assertEquals(1, inserted.getStatus());
        assertEquals(0, inserted.getIsBuiltin());
    }

    @Test
    @DisplayName("删除角色：已分配用户时抛 18016 且提示用户数")
    void delete_roleAssignedToUsers_rejected() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(2L);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> roleManageService.delete(ROLE_ID));

        assertEquals(18016, exception.getCode());
        assertTrue(exception.getMessage().contains("销售经理"));
        assertTrue(exception.getMessage().contains("2"));
        verify(sysRoleMapper, never()).deleteById(ROLE_ID);
    }

    @Test
    @DisplayName("删除角色：内置角色抛 18017")
    void delete_builtinRole_rejected() {
        SysRole builtin = normalRole();
        builtin.setIsBuiltin(1);
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(builtin);

        BusinessException exception = assertThrows(BusinessException.class,
            () -> roleManageService.delete(ROLE_ID));

        assertEquals(18017, exception.getCode());
        verify(sysRoleMapper, never()).deleteById(ROLE_ID);
    }

    @Test
    @DisplayName("删除角色：未引用时逻辑删除并级联清理授权与数据范围")
    void delete_success_cascadesAssociations() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);

        roleManageService.delete(ROLE_ID);

        verify(sysRoleMapper).deleteById(ROLE_ID);
        verify(sysRoleMenuMapper).delete(any());
        verify(sysRoleDataScopeMapper).delete(any());
    }

    @Test
    @DisplayName("分配菜单：全删全插 sys_role_menu 并统计权限点数")
    void assignMenus_deletesAllThenInserts() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        SysMenu menuWithPerms = new SysMenu();
        menuWithPerms.setId(1L);
        menuWithPerms.setPerms("system:user:list");
        SysMenu plainMenu = new SysMenu();
        plainMenu.setId(2L);
        when(sysMenuMapper.selectList(any())).thenReturn(List.of(menuWithPerms, plainMenu));

        RoleMenuAssignedVO vo = roleManageService.assignMenus(ROLE_ID, new RoleAssignMenuDTO(List.of(1L, 2L, 2L)));

        verify(sysRoleMenuMapper).delete(any());
        ArgumentCaptor<SysRoleMenu> captor = ArgumentCaptor.forClass(SysRoleMenu.class);
        verify(sysRoleMenuMapper, times(2)).insert(captor.capture());
        List<SysRoleMenu> inserted = captor.getAllValues();
        assertEquals(ROLE_ID, inserted.get(0).getRoleId());
        assertEquals(1L, inserted.get(0).getMenuId());
        assertEquals(2L, inserted.get(1).getMenuId());
        assertEquals(List.of(1L, 2L), vo.menuIds());
        // 仅 1 个菜单携带权限点
        assertEquals(1, vo.permCount());
    }

    @Test
    @DisplayName("分配菜单：包含不存在的菜单 ID 时抛 18019")
    void assignMenus_menuNotFound_rejected() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        when(sysMenuMapper.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
            () -> roleManageService.assignMenus(ROLE_ID, new RoleAssignMenuDTO(List.of(1L, 2L))));

        assertEquals(18019, exception.getCode());
        verify(sysRoleMenuMapper, never()).delete(any());
        verify(sysRoleMenuMapper, never()).insert(any(SysRoleMenu.class));
    }

    @Test
    @DisplayName("配置数据范围：自定义档位未勾选组织/人员时抛 18018")
    void saveDataScopes_customWithoutIds_rejected() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());

        BusinessException exception = assertThrows(BusinessException.class, () ->
            roleManageService.saveDataScopes(ROLE_ID, new RoleDataScopeSaveDTO(
                List.of(new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 9, null, null, null)))));

        assertEquals(18018, exception.getCode());
        verify(sysRoleDataScopeMapper, never()).delete(any());
        verify(sysRoleDataScopeMapper, never()).insert(any(SysRoleDataScope.class));
    }

    @Test
    @DisplayName("配置数据范围：全删全插并写部门 ID 逗号串与掩码默认值")
    void saveDataScopes_deletesAllThenInserts() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        when(sysRoleDataScopeMapper.selectList(any())).thenReturn(List.of());

        roleManageService.saveDataScopes(ROLE_ID, new RoleDataScopeSaveDTO(List.of(
            new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 6, null, null, null),
            new RoleDataScopeItemDTO("INVENTORY", "WAREHOUSE", 9, List.of(5L, 8L), List.of(3L), 1))));

        verify(sysRoleDataScopeMapper).delete(any());
        ArgumentCaptor<SysRoleDataScope> captor = ArgumentCaptor.forClass(SysRoleDataScope.class);
        verify(sysRoleDataScopeMapper, times(2)).insert(captor.capture());
        List<SysRoleDataScope> inserted = captor.getAllValues();
        assertEquals("SALES_ORDER", inserted.get(0).getBizObject());
        assertEquals(6, inserted.get(0).getScopeType());
        assertNull(inserted.get(0).getDeptIds());
        assertEquals(0, inserted.get(0).getFieldMask());
        assertEquals("5,8", inserted.get(1).getDeptIds());
        assertEquals("3", inserted.get(1).getUserIds());
        assertEquals(1, inserted.get(1).getFieldMask());
    }

    @Test
    @DisplayName("修改角色：乐观锁冲突时抛 10601")
    void update_optimisticLockConflict_rejected() {
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(normalRole());
        when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class, () ->
            roleManageService.update(ROLE_ID, new RoleUpdateDTO("销售经理改名", null, null, null, null, 0)));

        assertEquals(10601, exception.getCode());
    }

    @Test
    @DisplayName("修改角色：成功更新后返回数据库更新时间")
    void update_success_returnsUpdateTime() {
        SysRole stored = normalRole();
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(stored);
        when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(1);
        SysRole latest = normalRole();
        latest.setUpdateTime(LocalDateTime.of(2026, 9, 27, 10, 30, 0));
        when(sysRoleMapper.selectById(ROLE_ID)).thenReturn(stored, latest);

        var vo = roleManageService.update(ROLE_ID,
            new RoleUpdateDTO("销售经理改名", 2, 3, null, "备注", 0));

        assertEquals("2026-09-27 10:30:00", vo.updateTime());
        assertEquals("销售经理改名", stored.getRoleName());
        assertEquals(2, stored.getRoleSort());
        assertEquals(3, stored.getDataScope());
    }
}
