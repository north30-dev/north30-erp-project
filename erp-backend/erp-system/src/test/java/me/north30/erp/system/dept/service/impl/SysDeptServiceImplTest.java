package me.north30.erp.system.dept.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dept.DeptTestFactory;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.support.MpTableInfoInit;
import me.north30.erp.system.user.service.SysUserService;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link SysDeptServiceImpl} 纯单元测试：存在性、编码唯一与删除前置守卫。
 */
@ExtendWith(MockitoExtension.class)
class SysDeptServiceImplTest {

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysUserService sysUserService;

    @InjectMocks
    private SysDeptServiceImpl service;

    @BeforeAll
    static void initMpTableInfo() {
        // requireDeptCodeAvailable/requireDeptHasNoChildren 内部构建 LambdaQueryWrapper，
        // 纯 Mockito 测试需预先注册实体列缓存
        MpTableInfoInit.init(SysDept.class);
    }

    @Nested
    @DisplayName("getById：根据 ID 获取部门")
    class GetByIdTest {

        @Test
        @DisplayName("部门存在，返回部门")
        void shouldReturnDept_whenDeptExists() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptMapper.selectById(1L)).willReturn(dept);

            // When
            SysDept actual = service.getById(1L);

            // Then
            assertThat(actual).isSameAs(dept);
            verify(sysDeptMapper).selectById(1L);
        }

        @Test
        @DisplayName("部门不存在，返回 null")
        void shouldReturnNull_whenDeptMissing() {
            // Given
            given(sysDeptMapper.selectById(99L)).willReturn(null);

            // When
            SysDept actual = service.getById(99L);

            // Then
            assertThat(actual).isNull();
        }
    }

    @Nested
    @DisplayName("requireDept：校验组织存在")
    class RequireDeptTest {

        @Test
        @DisplayName("组织存在，返回实体")
        void shouldReturnDept_whenDeptExists() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptMapper.selectById(1L)).willReturn(dept);

            // When
            SysDept actual = service.requireDept(1L);

            // Then
            assertThat(actual).isSameAs(dept);
        }

        @Test
        @DisplayName("组织不存在，抛 18024")
        void shouldThrow_whenDeptMissing() {
            // Given
            given(sysDeptMapper.selectById(99L)).willReturn(null);

            // When + Then
            assertThatThrownBy(() -> service.requireDept(99L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DEPT_NOT_FOUND.getCode()))
                .hasMessage("组织 99 不存在");
        }
    }

    @Nested
    @DisplayName("requireDeptCodeAvailable：校验组织编码唯一")
    class RequireDeptCodeAvailableTest {

        @Test
        @DisplayName("编码可用，正常返回")
        void shouldPass_whenCodeAvailable() {
            // Given
            given(sysDeptMapper.selectCount(any())).willReturn(0L);

            // When + Then：不抛异常
            service.requireDeptCodeAvailable("ORG001");
            verify(sysDeptMapper).selectCount(any());
        }

        @Test
        @DisplayName("编码已存在，抛 18026")
        void shouldThrow_whenCodeExists() {
            // Given
            given(sysDeptMapper.selectCount(any())).willReturn(1L);

            // When + Then
            assertThatThrownBy(() -> service.requireDeptCodeAvailable("ORG001"))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DEPT_CODE_EXISTS.getCode()))
                .hasMessage("组织编码 ORG001 已存在");
        }
    }

    @Nested
    @DisplayName("requireDeptHasNoChildren：删除前校验无下级组织")
    class RequireDeptHasNoChildrenTest {

        @Test
        @DisplayName("无下级组织，正常返回")
        void shouldPass_whenNoChildren() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptMapper.selectCount(any())).willReturn(0L);

            // When + Then：不抛异常
            service.requireDeptHasNoChildren(dept);
        }

        @Test
        @DisplayName("存在下级组织，抛 18025")
        void shouldThrow_whenHasChildren() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptMapper.selectCount(any())).willReturn(2L);

            // When + Then
            assertThatThrownBy(() -> service.requireDeptHasNoChildren(dept))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS.getCode()))
                .hasMessage("组织 总部 存在下级组织或绑定用户，不可删除");
        }
    }

    @Nested
    @DisplayName("requireDeptHasNoUsers：删除前校验无绑定用户")
    class RequireDeptHasNoUsersTest {

        @Test
        @DisplayName("无绑定用户，正常返回")
        void shouldPass_whenNoUsers() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysUserService.countByDeptId(1L)).willReturn(0L);

            // When + Then：不抛异常
            service.requireDeptHasNoUsers(dept);
        }

        @Test
        @DisplayName("存在绑定用户，抛 18025")
        void shouldThrow_whenHasUsers() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysUserService.countByDeptId(1L)).willReturn(3L);

            // When + Then
            assertThatThrownBy(() -> service.requireDeptHasNoUsers(dept))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS.getCode()))
                .hasMessage("组织 总部 存在下级组织或绑定用户，不可删除");
        }
    }
}
