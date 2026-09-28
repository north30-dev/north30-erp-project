package me.north30.erp.system.dept.service.impl;

import me.north30.erp.system.dept.DeptTestFactory;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * {@link SysDeptServiceImpl} 纯单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysDeptServiceImplTest {

    @Mock
    private SysDeptMapper sysDeptMapper;

    @InjectMocks
    private SysDeptServiceImpl service;

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
}
