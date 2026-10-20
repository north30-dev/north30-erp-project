package me.north30.erp.system.unit.role.service.impl;

import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.role.service.impl.SysUserRoleServiceImpl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * {@link SysUserRoleServiceImpl} 纯 Mockito 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysUserRoleServiceImplTest {

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @InjectMocks
    private SysUserRoleServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存，保证 Lambda 条件在纯单测环境可解析
        RoleTestFactory.initTableInfo();
    }

    @Nested
    @DisplayName("根据用户 ID 获取用户角色列表")
    class ListByUserIdTest {

        @Test   
        @DisplayName("根据用户 ID 获取用户角色列表，用户存在")
        void shouldReturnUserRoles_whenUserIdGiven() {
            // Given
            List<SysUserRole> userRoles = List.of(
                RoleTestFactory.sysUserRole(10L, 1L),
                RoleTestFactory.sysUserRole(10L, 2L));
            when(sysUserRoleMapper.selectList(any())).thenReturn(userRoles);

            // When
            List<SysUserRole> result = service.listByUserId(10L);

            // Then
            assertThat(result).isSameAs(userRoles);
        }
    }

    @Nested
    @DisplayName("创建用户角色")
    class CreateUserRoleTest {

        @Test   
        @DisplayName("创建用户角色时，插入成功")
        void shouldReturnTrue_whenInsertAffected() {
            // Given
            SysUserRole userRole = RoleTestFactory.sysUserRole(10L, 1L);
            when(sysUserRoleMapper.insert(userRole)).thenReturn(1);

            // When
            boolean result = service.createUserRole(userRole);

            // Then
            assertThat(result).isTrue();
        }

        @Test   
        @DisplayName("创建用户角色时，插入失败")
        void shouldReturnFalse_whenInsertNotAffected() {
            // Given
            SysUserRole userRole = RoleTestFactory.sysUserRole(10L, 1L);
            when(sysUserRoleMapper.insert(userRole)).thenReturn(0);

            // When
            boolean result = service.createUserRole(userRole);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("统计用户已分配角色数")
    class CountByUserIdTest {

        @Test   
        @DisplayName("统计用户已分配角色数时，计数非空直接返回")
        void shouldReturnCount_whenCountPresent() {
            // Given
            when(sysUserRoleMapper.selectCount(any())).thenReturn(2L);

            // When
            long count = service.countByUserId(10L);

            // Then
            assertThat(count).isEqualTo(2L);
        }

        @Test   
        @DisplayName("统计用户已分配角色数时，计数为 null 按 0 处理（非引用即允许删除）")
        void shouldReturnZero_whenCountNull() {
            // Given
            when(sysUserRoleMapper.selectCount(any())).thenReturn(null);

            // When
            long count = service.countByUserId(10L);

            // Then
            assertThat(count).isZero();
        }
    }
}
