package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.service.SysMenuService;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.converter.RoleConverterImpl;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.mapper.SysRoleMenuMapper;
import me.north30.erp.system.role.service.SysRoleService;
import me.north30.erp.system.role.service.SysUserRoleService;
import me.north30.erp.system.role.strategy.RoleAssembleStrategy;
import me.north30.erp.system.role.strategy.RoleGrantStrategy;
import me.north30.erp.system.role.strategy.RoleQueryStrategy;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleUpdatedVO;
import me.north30.erp.system.role.vo.RoleVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link RoleManageServiceImpl} 纯 Mockito 单元测试：不连 DB。
 * <p>Mapper 依赖中同域授权表（roleMenu/dataScope）经 {@link RoleGrantStrategy} 保留 Mapper mock；
 * 跨域依赖（角色存在性、用户-角色、菜单）按小 DDD 收敛后 mock 对应 Service。</p>
 */
@ExtendWith(MockitoExtension.class)
class RoleManageServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysRoleService sysRoleService;

    @Mock
    private SysUserRoleService sysUserRoleService;

    @Mock
    private SysMenuService sysMenuService;

    @Mock
    private SysRoleMenuMapper sysRoleMenuMapper;

    @Mock
    private SysRoleDataScopeMapper sysRoleDataScopeMapper;

    @Spy
    private final RoleConverter roleConverter = new RoleConverterImpl();

    private RoleManageServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存，保证 Lambda 条件在纯单测环境可解析
        RoleTestFactory.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        RoleGrantStrategy roleGrantStrategy =
            new RoleGrantStrategy(sysRoleMenuMapper, sysRoleDataScopeMapper, roleConverter);
        service = new RoleManageServiceImpl(sysRoleMapper, sysRoleService, sysUserRoleService,
            sysMenuService, sysRoleDataScopeMapper, new RoleQueryStrategy(), roleGrantStrategy,
            new RoleAssembleStrategy(), roleConverter);
    }

    @Nested
    @DisplayName("page：分页查询角色")
    class PageTest {

        @Test
        @DisplayName("分页查询角色，查询成功")
        void shouldReturnPageWithDerivedUserCount_andDefaultOrderByCreateTimeDesc() {
            // Given
            when(sysRoleMapper.selectPage(ArgumentMatchers.<Page<SysRole>>any(), any())).thenAnswer(inv -> {
                Page<SysRole> page = inv.getArgument(0);
                page.setRecords(List.of(
                    RoleTestFactory.sysRole(1L, "sales", "销售专员"),
                    RoleTestFactory.sysRole(2L, "keeper", "仓管员")));
                page.setTotal(5);
                return page;
            });
            when(sysUserRoleService.listByRoleIds(any())).thenReturn(List.of(
                RoleTestFactory.sysUserRole(100L, 1L),
                RoleTestFactory.sysUserRole(101L, 1L),
                RoleTestFactory.sysUserRole(102L, 2L)));
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 1, 10, null, null);

            // When
            PageResult<RoleVO> result = service.page(query);

            // Then
            assertThat(result.total()).isEqualTo(5L);
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(10L);
            assertThat(result.list()).hasSize(2);
            RoleVO first = result.list().get(0);
            assertThat(first.id()).isEqualTo(1L);
            assertThat(first.roleCode()).isEqualTo("sales");
            assertThat(first.roleName()).isEqualTo("销售专员");
            assertThat(first.roleSort()).isEqualTo(2);
            assertThat(first.dataScope()).isEqualTo(2);
            assertThat(first.isBuiltin()).isZero();
            assertThat(first.status()).isEqualTo(1);
            assertThat(first.userCount()).isEqualTo(2);
            assertThat(result.list().get(1).userCount()).isEqualTo(1);
            // 默认排序：create_time DESC
            verify(sysRoleMapper).selectPage(argThat((Page<SysRole> page) ->
                page.orders().size() == 1
                    && "create_time".equals(page.orders().get(0).getColumn())
                    && !page.orders().get(0).isAsc()), any());
        }

        @Test
        @DisplayName("分页查询角色，pageNum 小于 1 时抛出异常")
        void shouldThrow10001_whenPageNumLessThanOne() {
            // Given
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 0, 10, null, null);

            // When / Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10001))
                .hasMessage("分页参数非法：pageNum ≥ 1 且 1 ≤ pageSize ≤ 200");
            verifyNoInteractions(sysUserRoleService);
        }

        @Test
        @DisplayName("分页查询角色，pageSize 大于 200 时抛出异常")
        void shouldThrow10001_whenPageSizeExceedsMax() {
            // Given
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 1, 201, null, null);

            // When / Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10001))
                .hasMessage("分页参数非法：pageNum ≥ 1 且 1 ≤ pageSize ≤ 200");
        }

        @Test
        @DisplayName("分页查询角色，orderBy字段不在白名单时抛出异常")
        void shouldThrow10001_whenOrderByNotInWhitelist() {
            // Given
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 1, 10, "drop", null);

            // When / Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10001))
                .hasMessage("排序字段非法：drop");
        }

        @Test
        @DisplayName("分页查询角色，orderDirection字段不在白名单时抛出异常")
        void shouldThrow10001_whenOrderDirectionIllegal() {
            // Given
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 1, 10, "role_code", "ascx");

            // When / Then
            assertThatThrownBy(() -> service.page(query))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10001))
                .hasMessage("排序方向仅支持 ASC/DESC");
        }

        @Test
        @DisplayName("分页查询角色，orderBy字段为 role_code 时，按 role_code 升序排序")
        void shouldOrderAscending_whenDirectionAscAndUserCountZero() {
            // Given
            when(sysRoleMapper.selectPage(ArgumentMatchers.<Page<SysRole>>any(), any())).thenAnswer(inv -> {
                Page<SysRole> page = inv.getArgument(0);
                page.setRecords(List.of(RoleTestFactory.sysRole(1L, "sales", "销售专员")));
                page.setTotal(1);
                return page;
            });
            when(sysUserRoleService.listByRoleIds(any())).thenReturn(List.of());
            RolePageQueryDTO query = new RolePageQueryDTO("sales", null, 1, 1, 10, "role_code", "asc");

            // When
            PageResult<RoleVO> result = service.page(query);

            // Then
            assertThat(result.list()).hasSize(1);
            // 无用户-角色关联时计数取默认 0
            assertThat(result.list().get(0).userCount()).isZero();
            verify(sysRoleMapper).selectPage(argThat((Page<SysRole> page) ->
                page.orders().size() == 1
                    && "role_code".equals(page.orders().get(0).getColumn())
                    && page.orders().get(0).isAsc()), any());
        }

        @Test
        @DisplayName("分页查询角色，orderBy字段为 role_code 时，按 role_code 降序排序")
        void shouldReturnEmptyList_whenNoRolesMatch() {
            // Given
            when(sysRoleMapper.selectPage(ArgumentMatchers.<Page<SysRole>>any(), any())).thenAnswer(inv -> {
                Page<SysRole> page = inv.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });
            RolePageQueryDTO query = new RolePageQueryDTO(null, null, null, 1, 10, null, null);

            // When
            PageResult<RoleVO> result = service.page(query);

            // Then
            assertThat(result.total()).isZero();
            assertThat(result.list()).isEmpty();
            // 空页不触发 userCount 派生查询
            verifyNoInteractions(sysUserRoleService);
        }
    }

    @Nested
    @DisplayName("create：创建角色")
    class CreateTest {

        @Test
        @DisplayName("创建角色，返回分配的 ID")
        void shouldCreateRoleWithDefaultValues_andReturnAssignedId() {
            // Given
            when(sysRoleMapper.selectCount(any())).thenReturn(0L);
            when(sysRoleMapper.insert(any(SysRole.class))).thenAnswer(inv -> {
                inv.<SysRole>getArgument(0).setId(88L);
                return 1;
            });

            // When
            RoleCreatedVO vo = service.create(RoleTestFactory.validRoleCreateDTO());

            // Then
            assertThat(vo.id()).isEqualTo(88L);
            verify(sysRoleMapper).insert(argThat((SysRole role) ->
                "R001".equals(role.getRoleCode())
                    && "操作员".equals(role.getRoleName())
                    && Integer.valueOf(0).equals(role.getRoleSort())
                    && Integer.valueOf(1).equals(role.getDataScope())
                    && Integer.valueOf(0).equals(role.getIsBuiltin())
                    && Integer.valueOf(1).equals(role.getStatus())
                    && "备注".equals(role.getRemark())));
        }

        @Test
        @DisplayName("创建角色，角色编码已存在时抛出异常")
        void shouldThrow18015_whenRoleCodeExists() {
            // Given
            when(sysRoleMapper.selectCount(any())).thenReturn(1L);

            // When / Then
            assertThatThrownBy(() -> service.create(RoleTestFactory.validRoleCreateDTO()))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18015))
                .hasMessage("角色编码 R001 已存在");
            verify(sysRoleMapper, never()).insert(any(SysRole.class));
        }

        @Test
        @DisplayName("创建角色，数据范围配置非法时抛出异常")
        void shouldThrow18018_whenDataScopeIllegal() {
            // Given
            when(sysRoleMapper.selectCount(any())).thenReturn(0L);
            RoleCreateDTO dto = new RoleCreateDTO("R001", "操作员", null, 7, null, null);

            // When / Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18018))
                .hasMessage("数据范围配置非法：data_scope=7");
            verify(sysRoleMapper, never()).insert(any(SysRole.class));
        }
    }

    @Nested
    @DisplayName("update：更新角色")
    class UpdateTest {

        @Test
        @DisplayName("更新角色，返回更新后的最新时间")
        void shouldUpdatePartialFields_andReturnFormattedLatestUpdateTime() {
            // Given：requireRole 命中旧角色，更新后 selectById 回查取最新数据
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysRoleMapper.selectById(5L)).thenReturn(RoleTestFactory.latestRoleWithUpdateTime());
            when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(1);
            RoleUpdateDTO dto = new RoleUpdateDTO("销售主管", 7, 3, 0, "改备注", 5);

            // When
            RoleUpdatedVO vo = service.update(5L, dto);

            // Then
            assertThat(vo.updateTime()).isEqualTo("2026-09-01 08:30:00");
            verify(sysRoleMapper).updateById(argThat((SysRole role) ->
                Long.valueOf(5L).equals(role.getId())
                    && "销售主管".equals(role.getRoleName())
                    && Integer.valueOf(7).equals(role.getRoleSort())
                    && Integer.valueOf(3).equals(role.getDataScope())
                    && Integer.valueOf(0).equals(role.getStatus())
                    && "改备注".equals(role.getRemark())
                    && Integer.valueOf(5).equals(role.getVersion())
                    // role_code 不可修改：保持库中原值
                    && "R001".equals(role.getRoleCode())));
        }

        @Test
        @DisplayName("更新角色，角色不存在时抛出异常")
        void shouldThrow18014_whenRoleNotFound() {
            // Given
            when(sysRoleService.requireRole(5L)).thenThrow(
                new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 5 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.update(5L, new RoleUpdateDTO("销售主管", null, null, null, null, 5)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色 5 不存在");
        }

        @Test
        @DisplayName("更新角色，数据范围配置非法时抛出异常")
        void shouldThrow18018_whenDataScopeIllegal() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());

            // When / Then
            assertThatThrownBy(() -> service.update(5L, new RoleUpdateDTO(null, null, 7, null, null, 5)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18018))
                .hasMessage("数据范围配置非法：data_scope=7");
            verify(sysRoleMapper, never()).updateById(any(SysRole.class));
        }

        @Test
        @DisplayName("更新角色，乐观锁冲突时抛出异常")
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(0);

            // When / Then
            assertThatThrownBy(() -> service.update(5L, new RoleUpdateDTO("销售主管", null, null, null, null, 5)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10601))
                .hasMessage("数据已被其他操作修改，请重试");
        }

        @Test
        @DisplayName("更新角色，乐观锁冲突时抛出异常")
        void shouldReturnNullUpdateTime_whenRoleMissingAfterUpdate() {
            // Given：乐观锁通过后回查被并发删除（selectById 返回 null）
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysRoleMapper.selectById(5L)).thenReturn(null);
            when(sysRoleMapper.updateById(any(SysRole.class))).thenReturn(1);

            // When
            RoleUpdatedVO vo = service.update(5L, new RoleUpdateDTO("销售主管", null, null, null, null, 5));

            // Then
            assertThat(vo.updateTime()).isNull();
        }
    }

    @Nested
    @DisplayName("delete：删除角色")
    class DeleteTest {

        @Test
        @DisplayName("删除角色，返回删除后的最新时间")
        void shouldDeleteRoleWithCascadeCleanup_whenNoUserAssigned() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysUserRoleService.countUsersByRoleId(5L)).thenReturn(0L);

            // When
            DeleteResultVO vo = service.delete(5L);

            // Then
            assertThat(vo.id()).isEqualTo(5L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysRoleMapper).deleteById(5L);
            verify(sysRoleMenuMapper).delete(argThat(wrapper -> {
                LambdaQueryWrapper<SysRoleMenu> condition = (LambdaQueryWrapper<SysRoleMenu>) wrapper;
                condition.getSqlSegment();
                return condition.getParamNameValuePairs().containsValue(5L);
            }));
            verify(sysRoleDataScopeMapper).delete(argThat(wrapper -> {
                LambdaQueryWrapper<SysRoleDataScope> condition = (LambdaQueryWrapper<SysRoleDataScope>) wrapper;
                condition.getSqlSegment();
                return condition.getParamNameValuePairs().containsValue(5L);
            }));
        }

        @Test
        @DisplayName("删除角色，内置角色不可删除时抛出异常")
        void shouldThrow18017_whenBuiltinRole() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.builtinRole());

            // When / Then
            assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18017))
                .hasMessage("内置角色不可删除");
            verify(sysRoleMapper, never()).deleteById(5L);
            verifyNoInteractions(sysUserRoleService, sysRoleMenuMapper, sysRoleDataScopeMapper);
        }

        @Test
        @DisplayName("删除角色，角色已分配给用户时抛出异常")
        void shouldThrow18016_whenRoleAssignedToUsers() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysUserRoleService.countUsersByRoleId(5L)).thenReturn(2L);

            // When / Then
            assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18016))
                .hasMessage("角色 操作员 已分配给 2 个用户，不可删除");
            verify(sysRoleMapper, never()).deleteById(5L);
            verifyNoInteractions(sysRoleMenuMapper, sysRoleDataScopeMapper);
        }

        @Test
        @DisplayName("删除角色，角色不存在时抛出异常")
        void shouldThrow18014_whenRoleNotFound() {
            // Given
            when(sysRoleService.requireRole(5L)).thenThrow(
                new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 5 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.delete(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色 5 不存在");
        }
    }

    @Nested
    @DisplayName("assignMenus：授权角色菜单")
    class AssignMenusTest {

        @Test
        @DisplayName("授权角色菜单，返回授权后的最新时间")
        void shouldAssignDistinctMenus_andCountPermOnly() {
            // Given：perms 为 null 或空白不计入权限点数
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysMenuService.listByIds(any())).thenReturn(List.of(
                RoleTestFactory.sysMenu(1L, "sales:list"),
                RoleTestFactory.sysMenu(2L, null),
                RoleTestFactory.sysMenu(3L, "   ")));
            RoleAssignMenuDTO dto = new RoleAssignMenuDTO(List.of(1L, 2L, 2L, 3L));

            // When
            RoleMenuAssignedVO vo = service.assignMenus(5L, dto);

            // Then
            assertThat(vo.menuIds()).containsExactly(1L, 2L, 3L);
            // 仅 1 个非空白 perms（sales:list），null 与空白均不计入
            assertThat(vo.permCount()).isEqualTo(1);
            verify(sysRoleMenuMapper).delete(argThat(wrapper -> {
                LambdaQueryWrapper<SysRoleMenu> condition = (LambdaQueryWrapper<SysRoleMenu>) wrapper;
                condition.getSqlSegment();
                return condition.getParamNameValuePairs().containsValue(5L);
            }));
            verify(sysRoleMenuMapper).insert(argThat((SysRoleMenu roleMenu) ->
                Long.valueOf(5L).equals(roleMenu.getRoleId()) && Long.valueOf(1L).equals(roleMenu.getMenuId())));
            verify(sysRoleMenuMapper).insert(argThat((SysRoleMenu roleMenu) ->
                Long.valueOf(5L).equals(roleMenu.getRoleId()) && Long.valueOf(2L).equals(roleMenu.getMenuId())));
            verify(sysRoleMenuMapper).insert(argThat((SysRoleMenu roleMenu) ->
                Long.valueOf(5L).equals(roleMenu.getRoleId()) && Long.valueOf(3L).equals(roleMenu.getMenuId())));
        }

        @Test
        @DisplayName("收回全部授权，返回收回后的最新时间")
        void shouldRevokeAllMenus_whenMenuIdsEmpty() {
            // Given：空数组表示收回全部授权
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());

            // When
            RoleMenuAssignedVO vo = service.assignMenus(5L, new RoleAssignMenuDTO(List.of()));

            // Then
            assertThat(vo.menuIds()).isEmpty();
            assertThat(vo.permCount()).isZero();
            verify(sysRoleMenuMapper).delete(any(LambdaQueryWrapper.class));
            verify(sysRoleMenuMapper, never()).insert(any(SysRoleMenu.class));
            verifyNoInteractions(sysMenuService);
        }

        @Test
        @DisplayName("分配菜单，存在不存在或已删除的菜单时抛出异常")
        void shouldThrow18019_whenAnyMenuMissing() {
            // Given：IN 查询命中数 1 < 传入 2，说明存在不存在或已删除的菜单
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysMenuService.listByIds(any())).thenReturn(List.of(RoleTestFactory.sysMenu(1L, "sales:list")));

            // When / Then
            assertThatThrownBy(() -> service.assignMenus(5L, new RoleAssignMenuDTO(List.of(1L, 2L))))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18019))
                .hasMessage("菜单不存在或已删除");
            verifyNoInteractions(sysRoleMenuMapper);
        }

        @Test
        @DisplayName("授权角色菜单，角色不存在时抛出异常")
        void shouldThrow18014_whenRoleNotFound() {
            // Given
            when(sysRoleService.requireRole(5L)).thenThrow(
                new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 5 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.assignMenus(5L, new RoleAssignMenuDTO(List.of(1L))))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色 5 不存在");
            verifyNoInteractions(sysMenuService, sysRoleMenuMapper);
        }
    }

    @Nested
    @DisplayName("保存角色数据权限范围")
    class SaveDataScopesTest {

        @Test
        @DisplayName("保存角色数据权限范围时，返回保存后的最新数据范围")
        void shouldSaveScopesWithJoinedIds_andReturnListedScopes() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysRoleDataScopeMapper.selectList(any())).thenReturn(List.of(
                RoleTestFactory.sysRoleDataScope("SALES_ORDER", "CREATOR", 9, "1,2", "7", 1),
                RoleTestFactory.sysRoleDataScope("INVENTORY", "WAREHOUSE", 1, null, null, 0)));
            RoleDataScopeSaveDTO dto = new RoleDataScopeSaveDTO(List.of(
                new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 9, List.of(1L, 2L), List.of(7L), 1),
                new RoleDataScopeItemDTO("INVENTORY", "WAREHOUSE", 1, null, null, null)));

            // When
            RoleDataScopeVO vo = service.saveDataScopes(5L, dto);

            // Then
            verify(sysRoleDataScopeMapper).delete(argThat(wrapper -> {
                LambdaQueryWrapper<SysRoleDataScope> condition = (LambdaQueryWrapper<SysRoleDataScope>) wrapper;
                condition.getSqlSegment();
                return condition.getParamNameValuePairs().containsValue(5L);
            }));
            verify(sysRoleDataScopeMapper).insert(argThat((SysRoleDataScope scope) ->
                Long.valueOf(5L).equals(scope.getRoleId())
                    && "SALES_ORDER".equals(scope.getBizObject())
                    && "CREATOR".equals(scope.getFilterDimension())
                    && Integer.valueOf(9).equals(scope.getScopeType())
                    && "1,2".equals(scope.getDeptIds())
                    && "7".equals(scope.getUserIds())
                    && Integer.valueOf(1).equals(scope.getFieldMask())));
            // fieldMask 缺省置 0、空集合序列化为 null
            verify(sysRoleDataScopeMapper).insert(argThat((SysRoleDataScope scope) ->
                "INVENTORY".equals(scope.getBizObject())
                    && Integer.valueOf(1).equals(scope.getScopeType())
                    && scope.getDeptIds() == null
                    && scope.getUserIds() == null
                    && Integer.valueOf(0).equals(scope.getFieldMask())));
            assertThat(vo.dataScope()).isEqualTo(2);
            assertThat(vo.scopes()).hasSize(2);
            assertThat(vo.scopes().get(0).deptIds()).containsExactly(1L, 2L);
            assertThat(vo.scopes().get(0).userIds()).containsExactly(7L);
            assertThat(vo.scopes().get(0).fieldMask()).isEqualTo(1);
            assertThat(vo.scopes().get(1).deptIds()).isEmpty();
            assertThat(vo.scopes().get(1).fieldMask()).isZero();
        }

        @Test
        @DisplayName("保存数据范围，范围类型非法时抛出异常")
        void shouldThrow18018_whenScopeTypeIllegal() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            RoleDataScopeSaveDTO dto = new RoleDataScopeSaveDTO(List.of(
                new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 7, null, null, null)));

            // When / Then
            assertThatThrownBy(() -> service.saveDataScopes(5L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18018))
                .hasMessage("数据范围配置非法：scope_type=7");
            verifyNoInteractions(sysRoleDataScopeMapper);
        }
        @Test
        @DisplayName("保存数据范围，自定义范围（9）必须至少勾选组织或人员时抛出异常")
        void shouldThrow18018_whenCustomScopeWithoutDeptAndUser() {
            // Given：自定义范围（9）必须至少勾选组织或人员
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            RoleDataScopeSaveDTO dto = new RoleDataScopeSaveDTO(List.of(
                new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 9, null, List.of(), null)));

            // When / Then
            assertThatThrownBy(() -> service.saveDataScopes(5L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18018))
                .hasMessage("数据范围配置非法：自定义范围必须至少勾选组织或人员");
            verifyNoInteractions(sysRoleDataScopeMapper);
        }

        @Test
        @DisplayName("保存数据范围，角色不存在时抛出异常")
        void shouldThrow18014_whenRoleNotFound() {
            // Given
            when(sysRoleService.requireRole(5L)).thenThrow(
                new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 5 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.saveDataScopes(5L,
                new RoleDataScopeSaveDTO(List.of(
                    new RoleDataScopeItemDTO("SALES_ORDER", "CREATOR", 1, null, null, null)))))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色 5 不存在");
        }
    }

    @Nested
    @DisplayName("listDataScopes：查询角色数据范围")
    class ListDataScopesTest {

        @Test
        @DisplayName("查询数据范围，返回解析后的组织 ID 列表和人员 ID 列表")
        void shouldReturnRoleDataScopeWithParsedIds() {
            // Given
            SysRole role = RoleTestFactory.sysRole();
            role.setDataScope(9);
            when(sysRoleService.requireRole(5L)).thenReturn(role);
            when(sysRoleDataScopeMapper.selectList(any())).thenReturn(List.of(
                RoleTestFactory.sysRoleDataScope("SALES_ORDER", "CREATOR", 9, "1, 2", null, 2),
                RoleTestFactory.sysRoleDataScope("INVENTORY", "WAREHOUSE", 1, "", "7", 0)));

            // When
            RoleDataScopeVO vo = service.listDataScopes(5L);

            // Then
            assertThat(vo.dataScope()).isEqualTo(9);
            assertThat(vo.scopes()).hasSize(2);
            // ID 串解析容忍空白段，空白串解析为空集合
            assertThat(vo.scopes().get(0).deptIds()).containsExactly(1L, 2L);
            assertThat(vo.scopes().get(0).userIds()).isEmpty();
            assertThat(vo.scopes().get(0).fieldMask()).isEqualTo(2);
            assertThat(vo.scopes().get(1).deptIds()).isEmpty();
            assertThat(vo.scopes().get(1).userIds()).containsExactly(7L);
        }

        @Test
        @DisplayName("查询数据范围，角色未配置时返回空列表")
        void shouldReturnEmptyScopes_whenNoConfigSaved() {
            // Given
            when(sysRoleService.requireRole(5L)).thenReturn(RoleTestFactory.sysRole());
            when(sysRoleDataScopeMapper.selectList(any())).thenReturn(List.of());

            // When
            RoleDataScopeVO vo = service.listDataScopes(5L);

            // Then
            assertThat(vo.dataScope()).isEqualTo(2);
            assertThat(vo.scopes()).isEmpty();
        }

        @Test
        @DisplayName("查询数据范围，角色不存在时抛出异常")
        void shouldThrow18014_whenRoleNotFound() {
            // Given
            when(sysRoleService.requireRole(5L)).thenThrow(
                new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 5 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.listDataScopes(5L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色 5 不存在");
        }
    }
}
