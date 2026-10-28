package me.north30.erp.system.unit.menu.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.MenuTreeUtil;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.unit.menu.MenuTestFactory;
import me.north30.erp.system.menu.converter.MenuConverter;
import me.north30.erp.system.menu.converter.MenuConverterImpl;
import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.entity.SysMenu;
import me.north30.erp.system.menu.mapper.SysMenuMapper;
import me.north30.erp.system.menu.service.SysMenuDomainService;
import me.north30.erp.system.menu.service.impl.SysMenuApplicationServiceImpl;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.role.service.SysRoleMenuService;
import me.north30.erp.system.unit.security.support.MpTableInfoInit;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link SysMenuApplicationServiceImpl} 纯单元测试：树组装、类型/父级/perms 校验、乐观锁与删除约束。
 */
@ExtendWith(MockitoExtension.class)
class SysMenuApplicationServiceImplTest {

    @Mock
    private SysMenuMapper sysMenuMapper;

    @Mock
    private SysMenuDomainService sysMenuDomainService;

    @Mock
    private SysRoleMenuService sysRoleMenuService;

    @Spy
    private final MenuConverter menuConverter = new MenuConverterImpl();

    private MenuTreeUtil menuTreeUtil;

    private SysMenuApplicationServiceImpl service;

    @Captor
    private ArgumentCaptor<LambdaQueryWrapper<SysMenu>> menuWrapperCaptor;

    @Captor
    private ArgumentCaptor<SysMenu> menuCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysMenu.class);
    }

    @BeforeEach
    void setUp() {
        menuTreeUtil = new MenuTreeUtil(new MenuConverterImpl());
        service = new SysMenuApplicationServiceImpl(sysMenuMapper, sysMenuDomainService, sysRoleMenuService,
            menuTreeUtil, menuConverter);
    }

    @Nested
    @DisplayName("tree：组装菜单树")
    class TreeTest {

        @Test   
        @DisplayName("组装菜单树时，返回按 sort 升序的菜单树")
        void shouldBuildHierarchySortedBySort_whenMenusExist() {
            // Given：两个顶级目录与各自下级，menuSort 决定同级顺序
            SysMenu dirA = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 2);
            SysMenu dirB = MenuTestFactory.dirMenu(2L, "基础数据", 0L, 1);
            SysMenu menuA1 = MenuTestFactory.pageMenu(3L, "用户管理", 1L, "system/user/index", "system:user:list", 2);
            SysMenu menuA2 = MenuTestFactory.pageMenu(4L, "角色管理", 1L, "system/role/index", "system:role:list", 1);
            SysMenu button = MenuTestFactory.buttonMenu(5L, "新增用户", 3L, "system:user:create", 1);
            given(sysMenuMapper.selectList(any())).willReturn(List.of(dirA, dirB, menuA1, menuA2, button));

            // When
            List<MenuNodeVO> tree = service.tree(MenuTestFactory.treeQueryDTO(null, null, null));

            // Then：根节点按 sort 升序，子节点挂载正确
            assertThat(tree).extracting(MenuNodeVO::id).containsExactly(2L, 1L);
            assertThat(tree.get(1).children())
                .extracting(MenuNodeVO::id)
                .containsExactly(4L, 3L);
            assertThat(tree.get(1).children().get(1).children())
                .extracting(MenuNodeVO::id)
                .containsExactly(5L);
            // 无子节点的目录 children 保持 null
            assertThat(tree.get(0).children()).isNull();
        }

        @Test   
        @DisplayName("组装菜单树时，无菜单记录返回空树")
        void shouldReturnEmpty_whenNoMenus() {
            // Given
            given(sysMenuMapper.selectList(any())).willReturn(List.of());

            // When
            List<MenuNodeVO> tree = service.tree(MenuTestFactory.treeQueryDTO(null, null, null));

            // Then
            assertThat(tree).isEmpty();
        }

        @Test   
        @DisplayName("组装菜单树时，应用查询条件（菜单名称、状态）")
        void shouldApplyQueryConditions_whenQueryHasFilters() {
            // Given：携带菜单名称与状态过滤条件
            given(sysMenuMapper.selectList(any())).willReturn(List.of());

            // When
            service.tree(MenuTestFactory.treeQueryDTO("系统", null, 1));

            // Then：条件值已写入查询参数（MP 条件参数在渲染 SQL 片段时才落参，需先触发渲染）
            verify(sysMenuMapper).selectList(menuWrapperCaptor.capture());
            LambdaQueryWrapper<SysMenu> wrapper = menuWrapperCaptor.getValue();
            wrapper.getSqlSegment();
            var params = wrapper.getParamNameValuePairs().values();
            assertThat(params).anySatisfy(value -> assertThat(String.valueOf(value)).contains("系统"));
            assertThat(params).contains(1);
        }
    }

    @Nested
    @DisplayName("create：创建菜单")
    class CreateTest {

        @Test   
        @DisplayName("创建顶级目录时，返回模拟主键回填后的 ID")
        void shouldInsertWithDefaultsAndReturnId_whenCreateRootDir() {
            // Given：顶级目录，menuSort/visible/status 为空触发默认值
            MenuCreateDTO dto = MenuTestFactory.dirCreateDTO("系统管理", 0L, "/system", null);
            given(sysMenuMapper.insert(any(SysMenu.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysMenu.class).setId(100L);
                return 1;
            });

            // When
            var vo = service.create(dto);

            // Then：返回模拟主键回填后的 ID
            assertThat(vo.id()).isEqualTo(100L);
            verify(sysMenuMapper).insert(menuCaptor.capture());
            SysMenu inserted = menuCaptor.getValue();
            assertThat(inserted.getMenuName()).isEqualTo("系统管理");
            assertThat(inserted.getParentId()).isZero();
            assertThat(inserted.getMenuType()).isEqualTo(1);
            assertThat(inserted.getPath()).isEqualTo("/system");
            assertThat(inserted.getMenuSort()).isZero();
            assertThat(inserted.getVisible()).isEqualTo(1);
            assertThat(inserted.getStatus()).isEqualTo(1);
        }

        @Test   
        @DisplayName("创建按钮时，清空 path/component 字段")
        void shouldClearRouteAndComponent_whenCreateButton() {
            // Given：按钮类型即使传了 path/component 也应被清空，父级/perms 守卫由 domainService mock 放行
            MenuCreateDTO dto = MenuTestFactory.createDTO("新增用户", 3L, 3, "/user/create", "user/create.vue",
                "system:user:create", 1, null);
            given(sysMenuMapper.insert(any(SysMenu.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysMenu.class).setId(101L);
                return 1;
            });

            // When
            var vo = service.create(dto);

            // Then
            assertThat(vo.id()).isEqualTo(101L);
            verify(sysMenuMapper).insert(menuCaptor.capture());
            assertThat(menuCaptor.getValue().getPath()).isNull();
            assertThat(menuCaptor.getValue().getComponent()).isNull();
            assertThat(menuCaptor.getValue().getPerms()).isEqualTo("system:user:create");
        }

        @Test   
        @DisplayName("创建子菜单时，上级菜单不存在抛出异常")
        void shouldThrow_whenParentMissing() {
            // Given：父级守卫收敛进 SysMenuDomainService，mock 抛出领域异常
            MenuCreateDTO dto = MenuTestFactory.dirCreateDTO("孤儿目录", 99L, "/orphan", 1);
            willThrow(new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除"))
                .given(sysMenuDomainService).requireValidParent(99L, 1);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("上级菜单不存在或已删除");
            verify(sysMenuMapper, never()).insert(any(SysMenu.class));
        }

        @Test   
        @DisplayName("创建按钮时，抛出异常")
        void shouldThrow_whenParentIsButton() {
            // Given：按钮下不可再建子节点，父级守卫收敛进 SysMenuDomainService
            MenuCreateDTO dto = MenuTestFactory.dirCreateDTO("四级节点", 9L, "/too-deep", 1);
            willThrow(new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED))
                .given(sysMenuDomainService).requireValidParent(9L, 1);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
            verify(sysMenuMapper, never()).insert(any(SysMenu.class));
        }

        @Test   
        @DisplayName("创建子菜单时，深度超限抛出异常")
        void shouldThrow_whenDepthExceeded() {
            // Given：深度守卫收敛进 SysMenuDomainService，mock 抛出 18023
            MenuCreateDTO dto = MenuTestFactory.dirCreateDTO("超限节点", 9L, "/too-deep", 1);
            willThrow(new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED))
                .given(sysMenuDomainService).requireDepthWithinLimit(9L, 1);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
            verify(sysMenuMapper, never()).insert(any(SysMenu.class));
        }

        @Test   
        @DisplayName("创建菜单时，抛出异常")
        void shouldThrow_whenMenuTypeInvalid() {
            // Given：menuType 超出 1-3 范围
            MenuCreateDTO dto = MenuTestFactory.createDTO("非法类型", 0L, 9, "/x", null, null, 1, null);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("菜单类型非法：9");
            verifyNoInteractions(sysMenuMapper, sysRoleMenuService);
        }

        @Test   
        @DisplayName("创建目录时，抛出异常")
        void shouldThrow_whenDirMissingPath() {
            // Given
            MenuCreateDTO dto = MenuTestFactory.dirCreateDTO("无路由目录", 0L, null, 1);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("路由地址不能为空");
            verify(sysMenuMapper, never()).insert(any(SysMenu.class));
        }

        @Test   
        @DisplayName("创建菜单时，抛出异常")
        void shouldThrow_whenPermsDuplicated() {
            // Given：perms 唯一守卫收敛进 SysMenuDomainService，mock 抛出领域异常
            MenuCreateDTO dto = MenuTestFactory.createDTO("目录", 0L, 1, "/system", null, "sys:menu:add", 1, null);
            willThrow(new BusinessException(SystemErrorCode.MENU_PERMS_DUPLICATED, "权限标识 sys:menu:add 已存在"))
                .given(sysMenuDomainService).requirePermsAvailable("sys:menu:add", null);

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_PERMS_DUPLICATED.getCode()))
                .hasMessage("权限标识 sys:menu:add 已存在");
            verify(sysMenuMapper, never()).insert(any(SysMenu.class));
        }
    }

    @Nested
    @DisplayName("update：更新菜单")
    class UpdateTest {

        @Test   
        @DisplayName("更新菜单时，返回更新时间")
        void shouldUpdateFieldsAndReturnTime_whenSimpleUpdate() {
            // Given：不修改父级与类型，仅更新名称/路由/排序
            SysMenu menu = MenuTestFactory.dirMenu(1L, "旧名", 0L, 1);
            menu.setUpdateTime(LocalDateTime.of(2026, 9, 28, 10, 0, 0));
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(3, "新名", null, null, "/new", null, null, 2);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectById(1L)).willReturn(menu);
            given(sysMenuMapper.updateById(any(SysMenu.class))).willReturn(1);

            // When
            var vo = service.update(1L, dto);

            // Then：返回更新时间，且仅应用了非空字段
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 10:00:00");
            verify(sysMenuMapper).updateById(menuCaptor.capture());
            SysMenu updated = menuCaptor.getValue();
            assertThat(updated.getMenuName()).isEqualTo("新名");
            assertThat(updated.getPath()).isEqualTo("/new");
            assertThat(updated.getMenuSort()).isEqualTo(2);
            assertThat(updated.getVersion()).isEqualTo(3);
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenMenuNotFound() {
            // Given
            given(sysMenuDomainService.requireMenu(1L))
                .willThrow(new BusinessException(SystemErrorCode.MENU_NOT_FOUND));

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(1)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("菜单不存在");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenParentPointsToSelf() {
            // Given：合并后 parentId=1（自身），自指守卫收敛进 SysMenuDomainService，mock 抛出领域异常
            SysMenu menu = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            willThrow(new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身或自身下级"))
                .given(sysMenuDomainService).requireValidParent(1L, 1);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(1, null, 1L, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身或自身下级");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenParentIsOwnDescendant() {
            // Given：合并后 parentId=2，防环守卫收敛进 SysMenuDomainService
            SysMenu menu = MenuTestFactory.dirMenu(1L, "根目录", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            willThrow(new BusinessException(CommonErrorCode.PARAM_ERROR, "父级菜单不能指向自身或自身下级"))
                .given(sysMenuDomainService).requireValidParent(2L, 1);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(1, null, 2L, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("父级菜单不能指向自身或自身下级");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenNewParentMissing() {
            // Given：父级守卫收敛进 SysMenuDomainService，mock 抛出领域异常
            SysMenu menu = MenuTestFactory.dirMenu(1L, "根目录", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            willThrow(new BusinessException(SystemErrorCode.MENU_NOT_FOUND, "上级菜单不存在或已删除"))
                .given(sysMenuDomainService).requireValidParent(99L, 1);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(1, null, 99L, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("上级菜单不存在或已删除");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenNewParentIsButton() {
            // Given：父级守卫收敛进 SysMenuDomainService，mock 抛出领域异常
            SysMenu menu = MenuTestFactory.dirMenu(1L, "根目录", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            willThrow(new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED))
                .given(sysMenuDomainService).requireValidParent(9L, 1);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(1, null, 9L, null, null, null, null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_LEVEL_EXCEEDED.getCode()))
                .hasMessage("菜单层级超过上限（最多 4 级）");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("有子节点的菜单修改类型时抛出异常")
        void shouldThrow_whenChangeTypeWithChildren() {
            // Given：菜单(2) 改为目录(1)，且存在子节点
            SysMenu menu = MenuTestFactory.pageMenu(1L, "用户管理", 1L, "system/user/index", "system:user:list", 1);
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(1, null, null, 1, null, null, null, null);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectCount(any())).willReturn(2L);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("菜单存在子节点，不可修改类型，请先处理子节点");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("换父级时调用防环与子树深度守卫")
        void shouldInvokeCycleAndDepthGuards_whenParentChanged() {
            // Given：菜单从父级 1 换到父级 2，守卫由 domainService mock 放行
            SysMenu menu = MenuTestFactory.pageMenu(1L, "用户管理", 1L, "system/user/index", "system:user:list", 1);
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(2, null, 2L, null, null, null, null, null);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectById(1L)).willReturn(menu);
            given(sysMenuMapper.updateById(any(SysMenu.class))).willReturn(1);

            // When
            service.update(1L, dto);

            // Then
            verify(sysMenuDomainService).requireNotDescendant(1L, 2L);
            verify(sysMenuDomainService).requireSubtreeDepthWithinLimit(1L, 2L);
        }

        @Test   
        @DisplayName("未换父级时不调用防环与子树深度守卫")
        void shouldSkipDepthGuards_whenParentUnchanged() {
            // Given：仅改名称，父级不变（dto.parentId 为 null）
            SysMenu menu = MenuTestFactory.dirMenu(1L, "旧名", 0L, 1);
            menu.setUpdateTime(LocalDateTime.of(2026, 9, 28, 10, 0, 0));
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(3, "新名", null, null, "/new", null, null, 2);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectById(1L)).willReturn(menu);
            given(sysMenuMapper.updateById(any(SysMenu.class))).willReturn(1);

            // When
            service.update(1L, dto);

            // Then
            verify(sysMenuDomainService, never()).requireNotDescendant(anyLong(), anyLong());
            verify(sysMenuDomainService, never()).requireSubtreeDepthWithinLimit(anyLong(), anyLong());
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenPermsDuplicatedOnUpdate() {
            // Given：修改为已存在的 perms，唯一守卫收敛进 SysMenuDomainService
            SysMenu menu = MenuTestFactory.buttonMenu(1L, "新增按钮", 0L, "sys:menu:old", 1);
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(2, null, null, null, null, null, "sys:menu:dup", null);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            willThrow(new BusinessException(SystemErrorCode.MENU_PERMS_DUPLICATED, "权限标识 sys:menu:dup 已存在"))
                .given(sysMenuDomainService).requirePermsAvailable("sys:menu:dup", 1L);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_PERMS_DUPLICATED.getCode()))
                .hasMessage("权限标识 sys:menu:dup 已存在");
            verify(sysMenuMapper, never()).updateById(any(SysMenu.class));
        }

        @Test   
        @DisplayName("更新菜单时，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given：updateById 影响 0 行表示版本冲突
            SysMenu menu = MenuTestFactory.dirMenu(1L, "根目录", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.updateById(any(SysMenu.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, MenuTestFactory.updateDTO(5)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }

        @Test   
        @DisplayName("更新菜单时，清除路由和组件信息")
        void shouldClearRouteAndComponent_whenTypeChangedToButton() {
            // Given：页面改为按钮，path/component 应被清空
            SysMenu menu = MenuTestFactory.menu(1L, "用户管理", 0L, 2, "/system/user", "system/user/index",
                "system:user:list", 1);
            menu.setUpdateTime(LocalDateTime.of(2026, 9, 28, 11, 0, 0));
            MenuUpdateDTO dto = MenuTestFactory.updateDTO(2, null, null, 3, null, null, "system:user:create", null);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectCount(any())).willReturn(0L);
            given(sysMenuMapper.selectById(1L)).willReturn(menu);
            given(sysMenuMapper.updateById(any(SysMenu.class))).willReturn(1);

            // When
            var vo = service.update(1L, dto);

            // Then
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 11:00:00");
            verify(sysMenuMapper).updateById(menuCaptor.capture());
            SysMenu updated = menuCaptor.getValue();
            assertThat(updated.getMenuType()).isEqualTo(3);
            assertThat(updated.getPerms()).isEqualTo("system:user:create");
            assertThat(updated.getPath()).isNull();
            assertThat(updated.getComponent()).isNull();
        }
    }

    @Nested
    @DisplayName("删除菜单")
    class DeleteTest {

        @Test   
        @DisplayName("删除菜单时，抛出异常")
        void shouldDelete_whenNoChildAndNoRoleRef() {
            // Given：无子节点且未被角色引用
            SysMenu menu = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectCount(any())).willReturn(0L);
            given(sysRoleMenuService.existsByMenuId(1L)).willReturn(false);
            given(sysMenuMapper.deleteById(1L)).willReturn(1);

            // When
            DeleteResultVO vo = service.delete(1L);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysMenuMapper).deleteById(1L);
        }

        @Test   
        @DisplayName("删除菜单时，抛出异常")
        void shouldThrow_whenHasChildren() {
            // Given：存在子节点
            SysMenu menu = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectCount(any())).willReturn(2L);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_HAS_CHILDREN.getCode()))
                .hasMessage("菜单 系统管理 存在子节点，不可删除");
            verify(sysMenuMapper, never()).deleteById(anyLong());
            verifyNoInteractions(sysRoleMenuService);
        }

        @Test   
        @DisplayName("删除菜单时，抛出异常")
        void shouldThrow_whenReferencedByRole() {
            // Given：无子节点但已被角色引用
            SysMenu menu = MenuTestFactory.dirMenu(1L, "系统管理", 0L, 1);
            given(sysMenuDomainService.requireMenu(1L)).willReturn(menu);
            given(sysMenuMapper.selectCount(any())).willReturn(0L);
            given(sysRoleMenuService.existsByMenuId(1L)).willReturn(true);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_REFERENCED_BY_ROLE.getCode()))
                .hasMessage("菜单 系统管理 已被角色引用，不可删除");
            verify(sysMenuMapper, never()).deleteById(anyLong());
        }

        @Test   
        @DisplayName("删除菜单时，抛出异常")
        void shouldThrow_whenMenuNotFound() {
            // Given
            given(sysMenuDomainService.requireMenu(1L))
                .willThrow(new BusinessException(SystemErrorCode.MENU_NOT_FOUND));

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.MENU_NOT_FOUND.getCode()))
                .hasMessage("菜单不存在");
            verifyNoInteractions(sysRoleMenuService);
        }
    }
}
