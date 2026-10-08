package me.north30.erp.system.dept.entity;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link SysDept#applyParent(SysDept)} 充血方法直测：顶级/下级祖级路径与层级重算、层级上限校验。
 */
class SysDeptTest {

    @Test
    @DisplayName("applyParent 传 null 表示顶级：parentId=0、层级 1、祖级路径 0")
    void shouldApplyRoot_whenParentNull() {
        // Given
        SysDept dept = new SysDept();
        dept.setId(10L);

        // When
        dept.applyParent(null);

        // Then
        assertThat(dept.getParentId()).isZero();
        assertThat(dept.getDeptLevel()).isEqualTo(1);
        assertThat(dept.getAncestors()).isEqualTo("0");
    }

    @Test
    @DisplayName("applyParent 挂到下级：层级 = 父级层级 + 1，祖级路径 = 父级祖级路径 + 父级 ID")
    void shouldComputeLevelAndAncestors_whenAppliedToParent() {
        // Given
        SysDept parent = new SysDept();
        parent.setId(5L);
        parent.setDeptLevel(1);
        parent.setAncestors("0");
        SysDept dept = new SysDept();

        // When
        dept.applyParent(parent);

        // Then
        assertThat(dept.getParentId()).isEqualTo(5L);
        assertThat(dept.getDeptLevel()).isEqualTo(2);
        assertThat(dept.getAncestors()).isEqualTo("0,5");
    }

    @Test
    @DisplayName("applyParent 父级已是第 5 级，抛 18027 层级超限")
    void shouldThrow_whenLevelExceedsLimit() {
        // Given：父级已是第 5 级
        SysDept parent = new SysDept();
        parent.setId(5L);
        parent.setDeptLevel(5);
        parent.setAncestors("0,1,2,3,4");
        SysDept dept = new SysDept();

        // When + Then
        assertThatThrownBy(() -> dept.applyParent(parent))
            .isInstanceOfSatisfying(BusinessException.class,
                ex -> assertThat(ex.getCode())
                    .isEqualTo(SystemManageErrorCode.DEPT_LEVEL_EXCEED.getCode()))
            .hasMessage("组织层级超过上限");
    }
}
