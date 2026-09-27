package me.north30.erp.system.core.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.core.dto.DeptCreateDTO;
import me.north30.erp.system.core.dto.DeptUpdateDTO;
import me.north30.erp.system.core.vo.DeptMutationVO;
import me.north30.erp.system.core.entity.SysDept;
import me.north30.erp.system.core.mapper.SysDeptMapper;
import me.north30.erp.system.core.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SysDeptManageServiceImpl 单元测试：
 * 覆盖父级环检测、删除有子部门/绑定用户被拒、编码唯一、层级超限与删除成功。
 */
@ExtendWith(MockitoExtension.class)
class SysDeptManageServiceImplTest {

    @Mock
    private SysDeptMapper sysDeptMapper;
    @Mock
    private SysUserMapper sysUserMapper;

    private SysDeptManageServiceImpl deptManageService;

    @BeforeEach
    void setUp() {
        deptManageService = new SysDeptManageServiceImpl(sysDeptMapper, sysUserMapper);
    }

    private SysDept dept(Long id, Long parentId, int level, String ancestors) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setDeptCode("ORG" + id);
        dept.setDeptName("组织" + id);
        dept.setParentId(parentId);
        dept.setDeptType(4);
        dept.setDeptLevel(level);
        dept.setAncestors(ancestors);
        dept.setDeptSort(0);
        dept.setStatus(1);
        dept.setVersion(0);
        return dept;
    }

    private DeptUpdateDTO updateDTO(Long parentId, Integer version) {
        return new DeptUpdateDTO("组织A", parentId, 4, null, null, null, null, null, version);
    }

    @Test
    @DisplayName("新增成功：顶级组织 ancestors=0、层级=1")
    void createRootSuccess() {
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.insert(any(SysDept.class))).thenAnswer(invocation -> {
            SysDept dept = invocation.getArgument(0);
            dept.setId(100L);
            return 1;
        });

        DeptMutationVO vo = deptManageService.create(
            new DeptCreateDTO("ORG100", "组织100", 0L, 1, null, null, null, null, null));

        assertEquals("0", vo.ancestors());
        assertEquals(1, vo.deptLevel());
        assertEquals(100L, vo.id());
    }

    @Test
    @DisplayName("新增失败：组织编码已存在抛 18026")
    void createWithDuplicateCode() {
        when(sysDeptMapper.selectCount(any())).thenReturn(1L);

        BusinessException ex = assertThrows(BusinessException.class, () -> deptManageService.create(
            new DeptCreateDTO("ORG001", "组织1", 0L, 1, null, null, null, null, null)));

        assertEquals(18026, ex.getCode());
    }

    @Test
    @DisplayName("新增失败：父级不存在抛 18024")
    void createWithMissingParent() {
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.selectById(99L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () -> deptManageService.create(
            new DeptCreateDTO("ORG100", "组织100", 99L, 1, null, null, null, null, null)));

        assertEquals(18024, ex.getCode());
    }

    @Test
    @DisplayName("新增失败：层级超过 5 级抛 18027")
    void createWithLevelExceed() {
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.selectById(9L)).thenReturn(dept(9L, 0L, 5, "0,1,2,3,8"));

        BusinessException ex = assertThrows(BusinessException.class, () -> deptManageService.create(
            new DeptCreateDTO("ORG100", "组织100", 9L, 1, null, null, null, null, null)));

        assertEquals(18027, ex.getCode());
    }

    @Test
    @DisplayName("修改失败：父级改为自身抛 10001")
    void updateWithSelfAsParent() {
        when(sysDeptMapper.selectById(1L)).thenReturn(dept(1L, 0L, 1, "0"));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(dept(1L, 0L, 1, "0")));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deptManageService.update(1L, updateDTO(1L, 0)));

        assertEquals(10001, ex.getCode());
    }

    @Test
    @DisplayName("修改失败：父级改为自身后代（环检测）抛 10001")
    void updateWithDescendantAsParent() {
        // 树：1(根) -> 2 -> 3，把 1 的父级改为 3 成环
        when(sysDeptMapper.selectById(1L)).thenReturn(dept(1L, 0L, 1, "0"));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(
            dept(1L, 0L, 1, "0"), dept(2L, 1L, 2, "0,1"), dept(3L, 2L, 3, "0,1,2")));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> deptManageService.update(1L, updateDTO(3L, 0)));

        assertEquals(10001, ex.getCode());
        verify(sysDeptMapper, never()).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("修改成功：移动到根后级联重算下级 ancestors 与层级")
    void updateWithCascadeRefresh() {
        when(sysDeptMapper.selectById(2L)).thenReturn(dept(2L, 1L, 2, "0,1"));
        when(sysDeptMapper.selectList(any())).thenReturn(List.of(
            dept(1L, 0L, 1, "0"), dept(2L, 1L, 2, "0,1"), dept(3L, 2L, 3, "0,1,2")));
        when(sysDeptMapper.updateById(any(SysDept.class))).thenReturn(1);

        DeptMutationVO vo = deptManageService.update(2L, updateDTO(0L, 0));

        assertEquals("0", vo.ancestors());
        assertEquals(1, vo.deptLevel());
        // 目标节点 + 1 个后代各更新一次
        verify(sysDeptMapper, org.mockito.Mockito.times(2)).updateById(any(SysDept.class));
    }

    @Test
    @DisplayName("删除失败：存在下级组织抛 18025")
    void deleteWithChildren() {
        when(sysDeptMapper.selectById(1L)).thenReturn(dept(1L, 0L, 1, "0"));
        when(sysDeptMapper.selectCount(any())).thenReturn(2L);

        BusinessException ex = assertThrows(BusinessException.class, () -> deptManageService.delete(1L));

        assertEquals(18025, ex.getCode());
    }

    @Test
    @DisplayName("删除失败：存在挂靠用户抛 18025")
    void deleteWithAttachedUsers() {
        when(sysDeptMapper.selectById(1L)).thenReturn(dept(1L, 0L, 1, "0"));
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(3L);

        BusinessException ex = assertThrows(BusinessException.class, () -> deptManageService.delete(1L));

        assertEquals(18025, ex.getCode());
        verify(sysDeptMapper, never()).deleteById(1L);
    }

    @Test
    @DisplayName("删除成功：无下级无用户时逻辑删除")
    void deleteSuccess() {
        when(sysDeptMapper.selectById(1L)).thenReturn(dept(1L, 0L, 1, "0"));
        when(sysDeptMapper.selectCount(any())).thenReturn(0L);
        when(sysUserMapper.selectCount(any())).thenReturn(0L);
        when(sysDeptMapper.deleteById(1L)).thenReturn(1);

        DeptMutationVO vo = deptManageService.delete(1L);

        assertEquals(1, vo.isDeleted());
        verify(sysDeptMapper).deleteById(1L);
    }
}
