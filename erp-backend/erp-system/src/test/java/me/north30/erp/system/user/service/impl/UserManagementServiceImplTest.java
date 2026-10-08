package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.SystemErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.common.enums.UserErrorCode;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.user.converter.UserConverter;
import me.north30.erp.system.user.converter.UserConverterImpl;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.user.service.SysUserService;
import me.north30.erp.system.user.strategy.UserAssembleStrategy;
import me.north30.erp.system.user.strategy.UserQueryStrategy;
import me.north30.erp.system.user.strategy.UserRoleStrategy;
import me.north30.erp.system.user.strategy.UserSessionRevokeStrategy;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
import me.north30.erp.system.user.vo.UserDeleteVO;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserResetPasswordVO;
import me.north30.erp.system.user.vo.UserStatusVO;
import me.north30.erp.system.user.vo.UserVO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link UserManagementServiceImpl} 纯 Mockito 单元测试：不连 DB/Redis。
 * 查询/装配/角色关联策略用真实实例（内部 Mapper mock，保持 Wrapper 断言能力），
 * 会话清理策略与 SysUserService 存在性校验用 mock。
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private UserSessionRevokeStrategy userSessionRevokeStrategy;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Spy
    private final UserConverter userConverter = new UserConverterImpl();

    private UserManagementServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存：LambdaUpdateWrapper.set 与 LambdaQueryWrapper.select 会急切解析 Lambda 列
        UserTestFactory.initTableInfo();
    }

    @BeforeEach
    void setUp() {
        // 真实策略实例 + mock Mapper：查询条件/装配/角色关联逻辑走真实代码路径
        UserQueryStrategy userQueryStrategy = new UserQueryStrategy(sysDeptMapper);
        UserAssembleStrategy userAssembleStrategy =
            new UserAssembleStrategy(sysDeptMapper, sysUserRoleMapper, sysRoleMapper, userConverter);
        UserRoleStrategy userRoleStrategy =
            new UserRoleStrategy(sysRoleMapper, sysUserRoleMapper, userConverter);
        service = new UserManagementServiceImpl(sysUserMapper, sysDeptMapper, passwordEncoder,
            userConverter, sysUserService, userQueryStrategy, userAssembleStrategy,
            userRoleStrategy, userSessionRevokeStrategy);
    }

    @Nested
    class PageTest {

        @Test
        void shouldReturnPageWithDefaultPaging_whenPageParamsAbsent() {
            // Given
            when(sysUserMapper.selectPage(ArgumentMatchers.<Page<SysUser>>any(), any())).thenAnswer(inv -> {
                Page<SysUser> page = inv.getArgument(0);
                page.setRecords(List.of(UserTestFactory.sysUser()));
                page.setTotal(1);
                return page;
            });
            when(sysDeptMapper.selectByIds(any())).thenReturn(List.of(UserTestFactory.sysDept()));
            when(sysUserRoleMapper.selectList(any()))
                .thenReturn(List.of(UserTestFactory.sysUserRole(10L, 20L)));
            when(sysRoleMapper.selectByIds(any())).thenReturn(List.of(UserTestFactory.sysRole(20L, "keeper")));
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, null, null);

            // When
            PageResult<UserVO> result = service.page(query);

            // Then
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(20L);
            assertThat(result.total()).isEqualTo(1L);
            assertThat(result.list()).hasSize(1);
            UserVO vo = result.list().get(0);
            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.username()).isEqualTo("zhangsan");
            assertThat(vo.phone()).isEqualTo("138****5678");
            assertThat(vo.deptName()).isEqualTo("生产部");
            assertThat(vo.roles()).containsExactly("keeper");
            assertThat(vo.lastLoginTime()).isEqualTo("2026-01-02 09:30:00");
            assertThat(vo.createTime()).isEqualTo("2026-01-01 08:00:00");
            verify(sysUserMapper).selectPage(argThat((Page<SysUser> page) ->
                page.getCurrent() == 1L && page.getSize() == 20L), any());
        }

        @Test
        void shouldClampPageSizeToMax_whenPageSizeExceedsLimit() {
            // Given
            when(sysUserMapper.selectPage(ArgumentMatchers.<Page<SysUser>>any(), any())).thenAnswer(inv -> {
                Page<SysUser> page = inv.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, 2, 500);

            // When
            PageResult<UserVO> result = service.page(query);

            // Then
            assertThat(result.pageNum()).isEqualTo(2L);
            assertThat(result.pageSize()).isEqualTo(200L);
            verify(sysUserMapper).selectPage(argThat((Page<SysUser> page) ->
                page.getCurrent() == 2L && page.getSize() == 200L), any());
        }

        @Test
        void shouldFallbackPageNumToDefault_whenPageNumLessThanOne() {
            // Given
            when(sysUserMapper.selectPage(ArgumentMatchers.<Page<SysUser>>any(), any())).thenAnswer(inv -> {
                Page<SysUser> page = inv.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, 0, 10);

            // When
            PageResult<UserVO> result = service.page(query);

            // Then
            assertThat(result.pageNum()).isEqualTo(1L);
            assertThat(result.pageSize()).isEqualTo(10L);
            verify(sysUserMapper).selectPage(argThat((Page<SysUser> page) ->
                page.getCurrent() == 1L && page.getSize() == 10L), any());
        }

        @Test
        void shouldFilterDeptsWithDescendants_whenDeptIdGiven() {
            // Given
            SysDept self = UserTestFactory.sysDept();
            self.setId(1L);
            self.setAncestors("0");
            SysDept child = UserTestFactory.sysDept();
            child.setId(5L);
            child.setAncestors("0,1");
            SysDept other = UserTestFactory.sysDept();
            other.setId(6L);
            other.setAncestors("0,2");
            SysDept blank = UserTestFactory.sysDept();
            blank.setId(7L);
            blank.setAncestors(" ");
            when(sysDeptMapper.selectList(any())).thenReturn(List.of(self, child, other, blank));
            when(sysUserMapper.selectPage(ArgumentMatchers.<Page<SysUser>>any(), any())).thenAnswer(inv -> {
                Page<SysUser> page = inv.getArgument(0);
                SysUser user = UserTestFactory.sysUser();
                user.setId(20L);
                user.setDeptId(null);
                page.setRecords(List.of(user));
                page.setTotal(1);
                return page;
            });
            when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());
            UserQueryDTO query = new UserQueryDTO(null, null, null, 1L, null, 1, 10);

            // When
            PageResult<UserVO> result = service.page(query);

            // Then
            assertThat(result.list()).hasSize(1);
            // 组织及下级一次全量查询后内存过滤：无循环查库（仅 1 次 selectList）
            verify(sysDeptMapper, never()).selectByIds(any());
            verify(sysRoleMapper, never()).selectByIds(any());
            verify(sysDeptMapper, times(1)).selectList(any());
            verify(sysUserMapper).selectPage(ArgumentMatchers.<Page<SysUser>>any(), argThat(wrapper -> {
                LambdaQueryWrapper<SysUser> condition = (LambdaQueryWrapper<SysUser>) wrapper;
                condition.getSqlSegment(); // 触发条件段求值，填充占位参数
                return condition.getParamNameValuePairs().containsValue(1L)
                    && condition.getParamNameValuePairs().containsValue(5L)
                    && !condition.getParamNameValuePairs().containsValue(6L)
                    && !condition.getParamNameValuePairs().containsValue(7L);
            }));
        }

        @Test
        void shouldReturnEmptyPage_whenNoUsersMatch() {
            // Given
            when(sysUserMapper.selectPage(ArgumentMatchers.<Page<SysUser>>any(), any())).thenAnswer(inv -> {
                Page<SysUser> page = inv.getArgument(0);
                page.setRecords(List.of());
                page.setTotal(0);
                return page;
            });
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, 1, 10);

            // When
            PageResult<UserVO> result = service.page(query);

            // Then
            assertThat(result.total()).isZero();
            assertThat(result.list()).isEmpty();
            verifyNoInteractions(sysDeptMapper, sysUserRoleMapper, sysRoleMapper);
        }
    }

    @Nested
    class GetDetailTest {

        @Test
        void shouldReturnDetailWithDistinctRolesAndMaskedPhone() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(
                UserTestFactory.sysUserRole(10L, 20L),
                UserTestFactory.sysUserRole(10L, 21L),
                UserTestFactory.sysUserRole(10L, null),
                UserTestFactory.sysUserRole(10L, 20L)));
            when(sysRoleMapper.selectByIds(any())).thenReturn(List.of(
                UserTestFactory.sysRole(20L, "admin"),
                UserTestFactory.sysRole(21L, "operator")));
            when(sysDeptMapper.selectById(3L)).thenReturn(UserTestFactory.sysDept());

            // When
            UserDetailVO vo = service.getDetail(10L);

            // Then
            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.username()).isEqualTo("zhangsan");
            assertThat(vo.phone()).isEqualTo("138****5678");
            assertThat(vo.deptId()).isEqualTo(3L);
            assertThat(vo.deptName()).isEqualTo("生产部");
            assertThat(vo.roleIds()).containsExactly(20L, 21L);
            assertThat(vo.roles()).containsExactly("admin", "operator");
            assertThat(vo.warehouseIds()).containsExactly(1L, 2L);
            assertThat(vo.status()).isEqualTo(1);
            assertThat(vo.isAdmin()).isZero();
            assertThat(vo.gender()).isEqualTo(1);
            assertThat(vo.loginFailCount()).isEqualTo(1);
            assertThat(vo.lastLoginTime()).isEqualTo("2026-01-02 09:30:00");
            assertThat(vo.createTime()).isEqualTo("2026-01-01 08:00:00");
            assertThat(vo.lockUntil()).isNull();
            assertThat(vo.passwordUpdateTime()).isNull();
        }

        @Test
        void shouldSkipDeptAndRoleQueries_whenUserHasNoDeptAndRoles() {
            // Given
            SysUser user = UserTestFactory.sysUser();
            user.setDeptId(null);
            user.setPhone(null);
            user.setWarehouseIds(null);
            when(sysUserService.requireUser(10L)).thenReturn(user);
            when(sysUserRoleMapper.selectList(any())).thenReturn(List.of());

            // When
            UserDetailVO vo = service.getDetail(10L);

            // Then
            assertThat(vo.phone()).isNull();
            assertThat(vo.deptName()).isNull();
            assertThat(vo.roleIds()).isEmpty();
            assertThat(vo.roles()).isEmpty();
            assertThat(vo.warehouseIds()).isNull();
            verifyNoInteractions(sysDeptMapper, sysRoleMapper);
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.getDetail(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
        }
    }

    @Nested
    class CreateTest {

        @Test
        void shouldCreateUserWithEncodedPasswordAndDefaultValues() {
            // Given
            when(sysDeptMapper.selectById(3L)).thenReturn(UserTestFactory.sysDept());
            when(sysRoleMapper.selectCount(any())).thenReturn(2L);
            when(passwordEncoder.encode("Passw0rd!")).thenReturn("encodedPwd");
            when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(inv -> {
                inv.<SysUser>getArgument(0).setId(100L);
                return 1;
            });
            UserCreateDTO dto = UserTestFactory.validUserCreateDTO();

            // When
            Long id = service.create(dto);

            // Then
            assertThat(id).isEqualTo(100L);
            verify(sysUserMapper).insert(argThat((SysUser user) ->
                "E001".equals(user.getUserCode())
                    && "zhangsan".equals(user.getUsername())
                    && "encodedPwd".equals(user.getPassword())
                    && "张三".equals(user.getRealName())
                    && Long.valueOf(3L).equals(user.getDeptId())
                    && "1,2".equals(user.getWarehouseIds())
                    && Integer.valueOf(0).equals(user.getGender())
                    && Integer.valueOf(1).equals(user.getStatus())
                    && Integer.valueOf(0).equals(user.getIsAdmin())
                    && user.getPasswordUpdateTime() != null));
            verify(sysUserRoleMapper).insert(argThat((SysUserRole userRole) ->
                Long.valueOf(100L).equals(userRole.getUserId()) && Long.valueOf(1L).equals(userRole.getRoleId())));
            verify(sysUserRoleMapper).insert(argThat((SysUserRole userRole) ->
                Long.valueOf(100L).equals(userRole.getUserId()) && Long.valueOf(2L).equals(userRole.getRoleId())));
        }

        @Test
        void shouldThrow18006_whenUsernameExists() {
            // Given
            doThrow(new BusinessException(UserErrorCode.USERNAME_EXISTS, "用户名 zhangsan 已存在"))
                .when(sysUserService).requireUsernameAvailable("zhangsan");

            // When / Then
            assertThatThrownBy(() -> service.create(UserTestFactory.validUserCreateDTO()))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18006))
                .hasMessage("用户名 zhangsan 已存在");
            verify(sysUserMapper, never()).insert(any(SysUser.class));
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        void shouldThrow18007_whenUserCodeExists() {
            // Given：用户名可用放行，用户编号已存在
            doThrow(new BusinessException(UserErrorCode.USER_CODE_EXISTS, "用户编号 E001 已存在"))
                .when(sysUserService).requireUserCodeAvailable("E001");

            // When / Then
            assertThatThrownBy(() -> service.create(UserTestFactory.validUserCreateDTO()))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18007))
                .hasMessage("用户编号 E001 已存在");
            verify(sysUserMapper, never()).insert(any(SysUser.class));
        }

        @Test
        void shouldThrow18009_whenInitialPasswordTooWeak() {
            // Given
            UserCreateDTO dto = new UserCreateDTO("E001", "zhangsan", "123", "张三", null,
                null, null, null, null, null, null, null);

            // When / Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18009))
                .hasMessage("新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类");
            verify(sysUserMapper, never()).insert(any(SysUser.class));
            verifyNoInteractions(passwordEncoder, sysDeptMapper, sysRoleMapper);
        }

        @Test
        void shouldThrow18024_whenDeptNotFound() {
            // Given
            when(sysDeptMapper.selectById(99L)).thenReturn(null);
            UserCreateDTO dto = new UserCreateDTO("E001", "zhangsan", "Passw0rd!", "张三", 99L,
                null, null, null, null, null, null, null);

            // When / Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18024))
                .hasMessage("组织 99 不存在");
            verify(sysUserMapper, never()).insert(any(SysUser.class));
            verifyNoInteractions(sysRoleMapper);
        }

        @Test
        void shouldThrow18014_whenAnyRoleMissing() {
            // Given：角色数计数 1 < 传入 2，说明存在不存在的角色
            when(sysRoleMapper.selectCount(any())).thenReturn(1L);
            UserCreateDTO dto = new UserCreateDTO("E001", "zhangsan", "Passw0rd!", "张三", null,
                null, null, null, null, null, List.of(1L, 2L), null);

            // When / Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色不存在，请刷新角色列表后重试");
            verify(sysUserMapper, never()).insert(any(SysUser.class));
        }
    }

    @Nested
    class UpdateTest {

        @Test
        void shouldUpdateWithVersionAndReturnFormattedTime() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysDeptMapper.selectById(3L)).thenReturn(UserTestFactory.sysDept());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
            UserUpdateDTO dto = new UserUpdateDTO("李四", 3L, List.of(1L, 2L),
                "13900001111", "ls@north30.com", 2, 0, "更新备注", 5);

            // When
            String updateTime = service.update(10L, dto);

            // Then
            assertThat(updateTime).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
            verify(sysUserMapper).updateById(argThat((SysUser entity) ->
                Long.valueOf(10L).equals(entity.getId())
                    && "李四".equals(entity.getRealName())
                    && Long.valueOf(3L).equals(entity.getDeptId())
                    && "1,2".equals(entity.getWarehouseIds())
                    && "13900001111".equals(entity.getPhone())
                    && "ls@north30.com".equals(entity.getEmail())
                    && Integer.valueOf(2).equals(entity.getGender())
                    && Integer.valueOf(0).equals(entity.getStatus())
                    && "更新备注".equals(entity.getRemark())
                    && Integer.valueOf(5).equals(entity.getVersion())
                    && entity.getUpdateTime() != null));
        }

        @Test
        void shouldThrow18012_whenDisableAdminUser() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.adminUser());
            UserUpdateDTO dto = new UserUpdateDTO(null, null, null, null, null, null, 0, null, 5);

            // When / Then
            assertThatThrownBy(() -> service.update(10L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18012))
                .hasMessage("内置超级管理员不可删除或停用");
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verifyNoInteractions(sysDeptMapper);
        }

        @Test
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(0);
            UserUpdateDTO dto = new UserUpdateDTO("李四", null, null, null, null, null, null, null, 5);

            // When / Then
            assertThatThrownBy(() -> service.update(10L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10601))
                .hasMessage("数据已被其他操作修改，请重试");
        }

        @Test
        void shouldThrow18024_whenDeptNotFound() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysDeptMapper.selectById(99L)).thenReturn(null);
            UserUpdateDTO dto = new UserUpdateDTO(null, 99L, null, null, null, null, null, null, 5);

            // When / Then
            assertThatThrownBy(() -> service.update(10L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18024))
                .hasMessage("组织 99 不存在");
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));
            UserUpdateDTO dto = new UserUpdateDTO("李四", null, null, null, null, null, null, null, 5);

            // When / Then
            assertThatThrownBy(() -> service.update(99L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
        }
    }

    @Nested
    class DeleteTest {

        @Test
        void shouldDeleteUserLogically_whenNoRoleAssigned() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);

            // When
            UserDeleteVO vo = service.delete(10L);

            // Then
            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            verify(sysUserMapper).deleteById(10L);
        }

        @Test
        void shouldThrow18012_whenDeleteAdminUser() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.adminUser());

            // When / Then
            assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18012))
                .hasMessage("内置超级管理员不可删除或停用");
            verify(sysUserMapper, never()).deleteById(10L);
            verifyNoInteractions(sysUserRoleMapper);
        }

        @Test
        void shouldThrow18013_whenUserHasAssignedRoles() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserRoleMapper.selectCount(any())).thenReturn(2L);

            // When / Then
            assertThatThrownBy(() -> service.delete(10L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18013))
                .hasMessage("用户 zhangsan 已分配角色，不可删除（请先解除角色分配或选择停用）");
            verify(sysUserMapper, never()).deleteById(10L);
        }

        @Test
        void shouldDelete_whenRoleCountIsNull() {
            // Given：策略对计数 null 的边界按 0 处理（非引用即允许删除）
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserRoleMapper.selectCount(any())).thenReturn(null);

            // When
            UserDeleteVO vo = service.delete(10L);

            // Then
            assertThat(vo.id()).isEqualTo(10L);
            verify(sysUserMapper).deleteById(10L);
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
        }
    }

    @Nested
    class ChangeStatusTest {

        @Test
        void shouldEnableUser_withoutTouchingSessions() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            // When
            UserStatusVO vo = service.changeStatus(10L, new UserStatusDTO(1, 2));

            // Then
            assertThat(vo.status()).isEqualTo(1);
            assertThat(vo.sessionRevoked()).isFalse();
            verify(sysUserMapper).updateById(argThat((SysUser entity) ->
                Long.valueOf(10L).equals(entity.getId())
                    && Integer.valueOf(1).equals(entity.getStatus())
                    && Integer.valueOf(2).equals(entity.getVersion())));
            verifyNoInteractions(userSessionRevokeStrategy);
        }

        @Test
        void shouldDisableUserAndRevokeSessions_whenStatusZero() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            // When
            UserStatusVO vo = service.changeStatus(10L, new UserStatusDTO(0, 3));

            // Then
            assertThat(vo.status()).isZero();
            assertThat(vo.sessionRevoked()).isTrue();
            verify(userSessionRevokeStrategy).revoke(10L);
            verify(sysUserMapper).updateById(argThat((SysUser entity) ->
                Integer.valueOf(0).equals(entity.getStatus())
                    && Integer.valueOf(3).equals(entity.getVersion())));
        }

        @Test
        void shouldStillReportRevoked_whenRedisUnavailable() {
            // Given：Redis 连接失败时策略内降级 WARN，服务层仍视为已触发失效（与旧行为一致）
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

            // When
            UserStatusVO vo = service.changeStatus(10L, new UserStatusDTO(0, 3));

            // Then
            assertThat(vo.status()).isZero();
            assertThat(vo.sessionRevoked()).isTrue();
            verify(sysUserMapper).updateById(any(SysUser.class));
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.changeStatus(99L, new UserStatusDTO(1, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
        }

        @Test
        void shouldThrow10001_whenStatusValueIllegal() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());

            // When / Then
            assertThatThrownBy(() -> service.changeStatus(10L, new UserStatusDTO(2, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10001))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verifyNoInteractions(userSessionRevokeStrategy);
        }

        @Test
        void shouldThrow18012_whenDisableAdminUser() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.adminUser());

            // When / Then
            assertThatThrownBy(() -> service.changeStatus(10L, new UserStatusDTO(0, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18012))
                .hasMessage("内置超级管理员不可删除或停用");
            verify(sysUserMapper, never()).updateById(any(SysUser.class));
            verifyNoInteractions(userSessionRevokeStrategy);
        }

        @Test
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(0);

            // When / Then
            assertThatThrownBy(() -> service.changeStatus(10L, new UserStatusDTO(1, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(10601))
                .hasMessage("数据已被其他操作修改，请重试");
            verifyNoInteractions(userSessionRevokeStrategy);
        }
    }

    @Nested
    class ResetPasswordTest {

        @Test
        void shouldResetWithExplicitPassword_andSetForceChangeFlag() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(passwordEncoder.encode("NewPass@123")).thenReturn("encodedNewPwd");

            // When
            UserResetPasswordVO vo = service.resetPassword(10L, new UserResetPasswordDTO("NewPass@123"));

            // Then
            assertThat(vo.initialPassword()).isEqualTo("NewPass@123");
            assertThat(vo.forceChangeOnLogin()).isTrue();
            verify(passwordEncoder).encode("NewPass@123");
            verify(sysUserMapper).update(any(SysUser.class), argThat(wrapper -> {
                LambdaUpdateWrapper<SysUser> update = (LambdaUpdateWrapper<SysUser>) wrapper;
                // set 为急切求值：占位参数中应包含加密后的口令
                return update.getParamNameValuePairs().containsValue("encodedNewPwd");
            }));
        }

        @Test
        void shouldGenerateComplexRandomPassword_whenNewPasswordBlank() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(passwordEncoder.encode(anyString())).thenReturn("encodedNewPwd");

            // When
            UserResetPasswordVO vo = service.resetPassword(10L, new UserResetPasswordDTO(null));

            // Then
            assertThat(vo.forceChangeOnLogin()).isTrue();
            assertThat(vo.initialPassword()).hasSize(12);
            assertThat(vo.initialPassword())
                .matches(".*[A-Z].*")
                .matches(".*[a-z].*")
                .matches(".*\\d.*")
                .matches(".*[!@#$%^&*].*");
            verify(passwordEncoder).encode(vo.initialPassword());
            verify(sysUserMapper).update(any(SysUser.class), argThat(wrapper -> {
                LambdaUpdateWrapper<SysUser> update = (LambdaUpdateWrapper<SysUser>) wrapper;
                return update.getParamNameValuePairs().containsValue("encodedNewPwd");
            }));
        }

        @Test
        void shouldThrow18009_whenExplicitPasswordTooWeak() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());

            // When / Then
            assertThatThrownBy(() -> service.resetPassword(10L, new UserResetPasswordDTO("123")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18009))
                .hasMessage("新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类");
            verify(sysUserMapper, never()).update(any(SysUser.class), any());
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.resetPassword(99L, new UserResetPasswordDTO(null)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
        }
    }

    @Nested
    class AssignRolesTest {

        @Test
        void shouldReplaceRolesWithDistinctIds_whenAssign() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysRoleMapper.selectCount(any())).thenReturn(2L);

            // When
            UserAssignRolesVO vo = service.assignRoles(10L, new UserAssignRolesDTO(List.of(2L, 2L, 3L)));

            // Then
            assertThat(vo.roleIds()).containsExactly(2L, 3L);
            assertThat(vo.permissionRefreshTip()).isEqualTo("权限变更将在 5 分钟内或重新登录后生效");
            verify(sysUserRoleMapper).delete(argThat(wrapper -> {
                LambdaQueryWrapper<SysUserRole> condition = (LambdaQueryWrapper<SysUserRole>) wrapper;
                condition.getSqlSegment();
                return condition.getParamNameValuePairs().containsValue(10L);
            }));
            verify(sysUserRoleMapper).insert(argThat((SysUserRole userRole) ->
                Long.valueOf(10L).equals(userRole.getUserId()) && Long.valueOf(2L).equals(userRole.getRoleId())));
            verify(sysUserRoleMapper).insert(argThat((SysUserRole userRole) ->
                Long.valueOf(10L).equals(userRole.getUserId()) && Long.valueOf(3L).equals(userRole.getRoleId())));
        }

        @Test
        void shouldClearAllRoles_whenEmptyRoleIds() {
            // Given：空数组表示清空全部角色
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());

            // When
            UserAssignRolesVO vo = service.assignRoles(10L, new UserAssignRolesDTO(List.of()));

            // Then
            assertThat(vo.roleIds()).isEmpty();
            verify(sysUserRoleMapper).delete(any(LambdaQueryWrapper.class));
            verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
            verify(sysRoleMapper, never()).selectCount(any());
        }

        @Test
        void shouldThrow18005_whenUserNotFound() {
            // Given
            when(sysUserService.requireUser(99L)).thenThrow(
                new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 99 不存在"));

            // When / Then
            assertThatThrownBy(() -> service.assignRoles(99L, new UserAssignRolesDTO(List.of(1L))))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18005))
                .hasMessage("用户 99 不存在");
            verifyNoInteractions(sysUserRoleMapper, sysRoleMapper);
        }

        @Test
        void shouldThrow18014_whenAnyRoleMissing() {
            // Given
            when(sysUserService.requireUser(10L)).thenReturn(UserTestFactory.sysUser());
            when(sysRoleMapper.selectCount(any())).thenReturn(1L);

            // When / Then
            assertThatThrownBy(() -> service.assignRoles(10L, new UserAssignRolesDTO(List.of(1L, 2L))))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(18014))
                .hasMessage("角色不存在，请刷新角色列表后重试");
            verify(sysUserRoleMapper, never()).delete(any(LambdaQueryWrapper.class));
            verify(sysUserRoleMapper, never()).insert(any(SysUserRole.class));
        }
    }

    @Nested
    class ExportCsvTest {

        @Test
        void shouldExportCsvWithBomMaskedPhoneAndEscapedField() {
            // Given
            SysUser second = UserTestFactory.sysUser();
            second.setId(2L);
            second.setUserCode("E002");
            second.setUsername("lisi");
            second.setRealName("李,四");
            second.setPhone(null);
            second.setEmail(null);
            second.setDeptId(null);
            second.setStatus(0);
            second.setIsAdmin(1);
            second.setLastLoginTime(null);
            when(sysUserMapper.selectList(any())).thenReturn(List.of(UserTestFactory.sysUser(), second));
            when(sysDeptMapper.selectByIds(any())).thenReturn(List.of(UserTestFactory.sysDept()));
            when(sysUserRoleMapper.selectList(any())).thenReturn(List.of(UserTestFactory.sysUserRole(10L, 5L)));
            when(sysRoleMapper.selectByIds(any())).thenReturn(List.of(UserTestFactory.sysRole(5L, "keeper")));
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, null, null);

            // When
            byte[] bytes = service.exportCsv(query);

            // Then
            String csv = new String(bytes, StandardCharsets.UTF_8);
            assertThat(csv).startsWith("\uFEFF");
            assertThat(csv).contains("用户ID,用户编号,用户名,姓名,联系电话,邮箱,所属组织,角色,状态,是否管理员,最后登录时间,创建时间\r\n");
            assertThat(csv).contains("10,E001,zhangsan,张三,138****5678,zs@north30.com,生产部,keeper,启用,否,"
                + "2026-01-02 09:30:00,2026-01-01 08:00:00\r\n");
            assertThat(csv).contains("2,E002,lisi,\"李,四\",,,,,停用,是,,2026-01-01 08:00:00\r\n");
        }

        @Test
        void shouldExportHeaderOnly_whenNoUsers() {
            // Given
            when(sysUserMapper.selectList(any())).thenReturn(List.of());
            UserQueryDTO query = new UserQueryDTO(null, null, null, null, null, null, null);

            // When
            byte[] bytes = service.exportCsv(query);

            // Then
            String csv = new String(bytes, StandardCharsets.UTF_8);
            assertThat(csv).isEqualTo("\uFEFF"
                + "用户ID,用户编号,用户名,姓名,联系电话,邮箱,所属组织,角色,状态,是否管理员,最后登录时间,创建时间\r\n");
            verifyNoInteractions(sysDeptMapper, sysUserRoleMapper, sysRoleMapper);
        }
    }
}
