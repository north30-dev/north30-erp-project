package me.north30.erp.system.role.service.impl;

import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.mapper.SysRoleMapper;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link SysRoleServiceImpl} 纯 Mockito 单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysRoleServiceImplTest {

    @Mock
    private SysRoleMapper sysRoleMapper;

    @InjectMocks
    private SysRoleServiceImpl service;

    @BeforeAll
    static void setUpTableInfo() {
        // 初始化 MP 表信息缓存，保证 Lambda 条件在纯单测环境可解析
        RoleTestFactory.initTableInfo();
    }

    @Nested
    @DisplayName("getById：根据 ID 获取角色")
    class GetByIdTest {

        @Test
        @DisplayName("根据 ID 获取角色，角色存在")
        void shouldReturnRole_whenIdExists() {
            // Given
            SysRole role = RoleTestFactory.sysRole();
            when(sysRoleMapper.selectById(5L)).thenReturn(role);

            // When
            SysRole result = service.getById(5L);

            // Then
            assertThat(result).isSameAs(role);
        }
    }

    @Nested
    @DisplayName("getByRoleCode：根据角色编码获取角色")
    class GetByRoleCodeTest {

        @Test
        @DisplayName("根据角色编码获取角色，角色存在")
        void shouldReturnRole_whenRoleCodeExists() {
            // Given
            SysRole role = RoleTestFactory.sysRole();
            when(sysRoleMapper.selectOne(any())).thenReturn(role);

            // When
            SysRole result = service.getByRoleCode("R001");

            // Then
            assertThat(result).isSameAs(role);
        }
    }

    @Nested
    @DisplayName("listByIds：根据 ID 列表获取角色列表") 
    class ListByIdsTest {

        @Test
        @DisplayName("根据 ID 列表获取角色列表，ID 列表为空")
        void shouldReturnEmpty_whenIdsNull() {
            // When
            List<SysRole> result = service.listByIds(null);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(sysRoleMapper);
        }

        @Test
        @DisplayName("根据 ID 列表获取角色列表，ID 列表为空")
        void shouldReturnEmpty_whenIdsEmpty() {
            // When
            List<SysRole> result = service.listByIds(List.of());

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(sysRoleMapper);
        }

        @Test   
        @DisplayName("根据 ID 列表获取角色列表，ID 列表非空")
        void shouldReturnRoles_whenIdsGiven() {
            // Given
            List<SysRole> roles = List.of(RoleTestFactory.sysRole(1L, "sales", "销售专员"),
                RoleTestFactory.sysRole(2L, "keeper", "仓管员"));
            when(sysRoleMapper.selectByIds(any())).thenReturn(roles);

            // When
            List<SysRole> result = service.listByIds(List.of(1L, 2L));

            // Then
            assertThat(result).isSameAs(roles);
        }
    }

    @Nested
    @DisplayName("createRole：创建角色")
    class CreateRoleTest {

        @Test
        @DisplayName("创建角色，插入成功")
        void shouldReturnTrue_whenInsertAffected() {
            // Given
            SysRole role = RoleTestFactory.sysRole();
            when(sysRoleMapper.insert(role)).thenReturn(1);

            // When
            boolean result = service.createRole(role);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("创建角色，插入未受影响")
        void shouldReturnFalse_whenInsertNotAffected() {
            // Given
            SysRole role = RoleTestFactory.sysRole();
            when(sysRoleMapper.insert(role)).thenReturn(0);

            // When
            boolean result = service.createRole(role);

            // Then
            assertThat(result).isFalse();
        }
    }
}
