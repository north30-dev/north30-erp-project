package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.UserAssignRolesDTO;
import me.north30.erp.system.core.dto.UserCreateDTO;
import me.north30.erp.system.core.dto.UserResetPasswordDTO;
import me.north30.erp.system.core.dto.UserStatusDTO;
import me.north30.erp.system.core.dto.UserUpdateDTO;
import me.north30.erp.system.core.entity.SysDept;
import me.north30.erp.system.core.entity.SysRole;
import me.north30.erp.system.core.entity.SysUser;
import me.north30.erp.system.core.entity.SysUserRole;
import me.north30.erp.system.core.mapper.SysDeptMapper;
import me.north30.erp.system.core.mapper.SysRoleMapper;
import me.north30.erp.system.core.mapper.SysUserMapper;
import me.north30.erp.system.core.mapper.SysUserRoleMapper;
import me.north30.erp.system.core.vo.UserAssignRolesVO;
import me.north30.erp.system.core.vo.UserDeleteVO;
import me.north30.erp.system.core.vo.UserResetPasswordVO;
import me.north30.erp.system.core.vo.UserStatusVO;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * UserManagementServiceImpl 用户管理单元测试：
 * 覆盖用户名重复、删除 admin 保护、删除存在角色分配拒绝、启停用（含会话失效）、
 * 分配角色（全删全插/角色不存在）、重置密码 BCrypt 生效与乐观锁冲突。
 * 各 Mapper 与 Redis 为 Mock，PasswordEncoder 为真实 BCrypt 实现。
 */
@ExtendWith(MockitoExtension.class)
class UserManagementServiceImplTest {

    private static final String RAW_PASSWORD = "Init@12345";

    @BeforeAll
    static void initTableInfo() {
        // LambdaQueryWrapper/LambdaUpdateWrapper 解析实体 Lambda 需要元数据缓存（无 Spring 容器时手工初始化）
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        assistant.setCurrentNamespace("test");
        TableInfoHelper.initTableInfo(assistant, SysUser.class);
        TableInfoHelper.initTableInfo(assistant, SysUserRole.class);
        TableInfoHelper.initTableInfo(assistant, SysRole.class);
        TableInfoHelper.initTableInfo(assistant, SysDept.class);
    }

    @Mock
    private SysUserMapper sysUserMapper;
    @Mock
    private SysUserRoleMapper sysUserRoleMapper;
    @Mock
    private SysRoleMapper sysRoleMapper;
    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Captor
    private ArgumentCaptor<LambdaUpdateWrapper<SysUser>> wrapperCaptor;

    private PasswordEncoder passwordEncoder;
    private UserManagementServiceImpl userManagementService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userManagementService = new UserManagementServiceImpl(
            sysUserMapper, sysUserRoleMapper, sysRoleMapper, sysDeptMapper, stringRedisTemplate, passwordEncoder);
    }

    private SysUser adminUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("系统管理员");
        user.setStatus(1);
        user.setIsAdmin(1);
        return user;
    }

    private SysUser normalUser() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setUsername("zhangsan");
        user.setRealName("张三");
        user.setStatus(1);
        user.setIsAdmin(0);
        return user;
    }

    private UserCreateDTO createDTO() {
        return new UserCreateDTO("E001", "zhangsan", RAW_PASSWORD, "张三",
            null, null, null, null, 1, 1, List.of(10L), null);
    }

    // ---------------------------------------------------------------- 新增

    @Test
    @DisplayName("新增用户：用户名已存在 → 18006")
    void create_usernameExists_rejected() {
        when(sysUserMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> userManagementService.create(createDTO()));
        assertEquals(18006, ex.getCode());
        verify(sysUserMapper, never()).insert(any(SysUser.class));
    }

    @Test
    @DisplayName("新增用户：成功 → 口令 BCrypt 加密并写入角色关联")
    void create_success_encodesPasswordAndWritesRoles() {
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);
        when(sysUserMapper.insert(any(SysUser.class))).thenAnswer(invocation -> {
            invocation.getArgument(0, SysUser.class).setId(99L);
            return 1;
        });

        Long id = userManagementService.create(createDTO());

        assertEquals(99L, id);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).insert(userCaptor.capture());
        SysUser inserted = userCaptor.getValue();
        assertNotEquals(RAW_PASSWORD, inserted.getPassword());
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, inserted.getPassword()));
        assertNotNull(inserted.getPasswordUpdateTime());
        assertEquals(0, inserted.getIsAdmin());
        verify(sysUserRoleMapper, times(1)).insert(any(SysUserRole.class));
    }

    // ---------------------------------------------------------------- 删除

    @Test
    @DisplayName("删除用户：内置 admin 不可删除 → 18012")
    void delete_admin_rejected() {
        when(sysUserMapper.selectById(1L)).thenReturn(adminUser());

        BusinessException ex = assertThrows(BusinessException.class, () -> userManagementService.delete(1L));
        assertEquals(18012, ex.getCode());
        verify(sysUserMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除用户：存在角色分配（被引用）→ 18013")
    void delete_withRoles_rejected() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> userManagementService.delete(2L));
        assertEquals(18013, ex.getCode());
        verify(sysUserMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("删除用户：无关联 → 逻辑删除成功")
    void delete_success_logicalDelete() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserRoleMapper.selectCount(any())).thenReturn(0L);

        UserDeleteVO vo = userManagementService.delete(2L);

        assertEquals(2L, vo.id());
        assertEquals(1, vo.isDeleted());
        verify(sysUserMapper).deleteById(2L);
    }

    // ---------------------------------------------------------------- 启停用

    @Test
    @DisplayName("启停用：内置 admin 不可停用 → 18012")
    void changeStatus_disableAdmin_rejected() {
        when(sysUserMapper.selectById(1L)).thenReturn(adminUser());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> userManagementService.changeStatus(1L, new UserStatusDTO(0, 1)));
        assertEquals(18012, ex.getCode());
        verify(sysUserMapper, never()).updateById(any(SysUser.class));
    }

    @Test
    @DisplayName("启停用：非法状态值 → 10001")
    void changeStatus_invalidStatus_paramError() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());

        BusinessException ex = assertThrows(BusinessException.class,
            () -> userManagementService.changeStatus(2L, new UserStatusDTO(2, 1)));
        assertEquals(10001, ex.getCode());
    }

    @Test
    @DisplayName("启停用：停用成功并使在线会话失效")
    void changeStatus_disable_revokesSessions() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);
        when(stringRedisTemplate.keys(anyString())).thenReturn(Set.of("system:session:2:jti-1"));

        UserStatusVO vo = userManagementService.changeStatus(2L, new UserStatusDTO(0, 3));

        assertEquals(0, vo.status());
        assertTrue(vo.sessionRevoked());
        verify(stringRedisTemplate).keys("system:session:2:*");
        verify(stringRedisTemplate).keys("system:session:refresh:2:*");
        verify(stringRedisTemplate, times(2)).delete(anyCollection());
    }

    @Test
    @DisplayName("启停用：启用成功，不动会话")
    void changeStatus_enable_keepsSessions() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        UserStatusVO vo = userManagementService.changeStatus(2L, new UserStatusDTO(1, 3));

        assertEquals(1, vo.status());
        assertFalse(vo.sessionRevoked());
        verify(stringRedisTemplate, never()).keys(anyString());
    }

    // ---------------------------------------------------------------- 分配角色

    @Test
    @DisplayName("分配角色：角色不存在 → 18014")
    void assignRoles_roleNotExists_rejected() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysRoleMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> userManagementService.assignRoles(2L, new UserAssignRolesDTO(List.of(10L, 11L))));
        assertEquals(18014, ex.getCode());
        verify(sysUserRoleMapper, never()).delete(any());
    }

    @Test
    @DisplayName("分配角色：全删全插覆盖旧分配")
    void assignRoles_success_fullReplace() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysRoleMapper.selectCount(any())).thenReturn(2L);

        UserAssignRolesVO vo = userManagementService.assignRoles(2L, new UserAssignRolesDTO(List.of(10L, 11L)));

        assertEquals(List.of(10L, 11L), vo.roleIds());
        assertEquals("权限变更将在 5 分钟内或重新登录后生效", vo.permissionRefreshTip());
        verify(sysUserRoleMapper).delete(any());
        verify(sysUserRoleMapper, times(2)).insert(any(SysUserRole.class));
    }

    // ---------------------------------------------------------------- 重置密码

    @Test
    @DisplayName("重置密码：缺省生成随机口令，BCrypt 落库且强制改密标记为 true")
    void resetPassword_generated_bcryptApplied() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.update(any(SysUser.class), any())).thenReturn(1);

        UserResetPasswordVO vo = userManagementService.resetPassword(2L, new UserResetPasswordDTO(null));

        assertNotNull(vo.initialPassword());
        assertTrue(vo.initialPassword().length() >= 8);
        assertTrue(vo.forceChangeOnLogin());
        verify(sysUserMapper).update(any(SysUser.class), wrapperCaptor.capture());
        // 从 wrapper 参数中取出 BCrypt 密文校验
        String storedHash = extractStoredPasswordHash(wrapperCaptor.getValue());
        assertTrue(passwordEncoder.matches(vo.initialPassword(), storedHash));
    }

    @Test
    @DisplayName("重置密码：指定新口令 → BCrypt 落库生效")
    void resetPassword_explicit_bcryptApplied() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.update(any(SysUser.class), any())).thenReturn(1);

        UserResetPasswordVO vo = userManagementService.resetPassword(2L, new UserResetPasswordDTO("NewPass@123"));

        assertEquals("NewPass@123", vo.initialPassword());
        assertTrue(vo.forceChangeOnLogin());
        verify(sysUserMapper).update(any(SysUser.class), wrapperCaptor.capture());
        String storedHash = extractStoredPasswordHash(wrapperCaptor.getValue());
        assertTrue(passwordEncoder.matches("NewPass@123", storedHash));
    }

    /**
     * 从 LambdaUpdateWrapper 预编译参数中提取 BCrypt 密文（$2 开头的字符串参数）。
     */
    private String extractStoredPasswordHash(LambdaUpdateWrapper<SysUser> wrapper) {
        return wrapper.getParamNameValuePairs().values().stream()
            .filter(value -> value instanceof String hash && hash.startsWith("$2"))
            .map(String.class::cast)
            .findFirst()
            .orElseThrow();
    }

    // ---------------------------------------------------------------- 修改

    @Test
    @DisplayName("修改用户：乐观锁冲突 → 10601")
    void update_versionConflict_conflictError() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(0);

        UserUpdateDTO dto = new UserUpdateDTO("李四", null, null, null, null, null, null, null, 5);
        BusinessException ex = assertThrows(BusinessException.class, () -> userManagementService.update(2L, dto));
        assertEquals(10601, ex.getCode());
    }

    @Test
    @DisplayName("修改用户：停用内置 admin → 18012")
    void update_disableAdmin_rejected() {
        when(sysUserMapper.selectById(1L)).thenReturn(adminUser());

        UserUpdateDTO dto = new UserUpdateDTO(null, null, null, null, null, null, 0, null, 1);
        BusinessException ex = assertThrows(BusinessException.class, () -> userManagementService.update(1L, dto));
        assertEquals(18012, ex.getCode());
    }

    @Test
    @DisplayName("修改用户：成功带乐观锁版本更新并返回更新时间")
    void update_success_withVersion() {
        when(sysUserMapper.selectById(2L)).thenReturn(normalUser());
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        UserUpdateDTO dto = new UserUpdateDTO("李四", null, null, null, null, 1, null, "备注", 5);
        String updateTime = userManagementService.update(2L, dto);

        assertNotNull(updateTime);
        ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(sysUserMapper).updateById(userCaptor.capture());
        assertEquals(5, userCaptor.getValue().getVersion());
        assertEquals("李四", userCaptor.getValue().getRealName());
        assertEquals(1, userCaptor.getValue().getGender());
    }
}
