package me.north30.erp.system.unit.dept.service.impl;

import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.common.util.DeptTreeUtil;
import me.north30.erp.system.unit.dept.DeptTestFactory;
import me.north30.erp.system.unit.security.support.MpTableInfoInit;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.converter.DeptConverterImpl;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptMoveDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptDomainService;
import me.north30.erp.system.dept.service.impl.SysDeptApplicationServiceImpl;
import me.north30.erp.system.dept.strategy.DeptTreeStrategy;
import me.north30.erp.system.dept.vo.DeptTreeVO;

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
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * {@link SysDeptApplicationServiceImpl} 纯单元测试：树查询、编码唯一、层级上限、
 * 父级环检测、级联重算与删除约束。
 */
@ExtendWith(MockitoExtension.class)
class SysDeptApplicationServiceImplTest {

    @Mock
    private SysDeptMapper sysDeptMapper;

    @Mock
    private SysDeptDomainService sysDeptService;

    @Spy
    private final DeptTreeUtil deptTreeUtil = new DeptTreeUtil(new DeptConverterImpl());

    @Spy
    private final DeptConverter deptConverter = new DeptConverterImpl();

    private DeptTreeStrategy deptTreeStrategy;

    private SysDeptApplicationServiceImpl service;

    @Captor
    private ArgumentCaptor<SysDept> deptCaptor;

    @BeforeAll
    static void initMpTableInfo() {
        // 服务内部构建 LambdaQueryWrapper，需预先注册实体列缓存
        MpTableInfoInit.init(SysDept.class);
    }

    @BeforeEach
    void setUp() {
        deptTreeStrategy = new DeptTreeStrategy(sysDeptMapper);
        service = new SysDeptApplicationServiceImpl(sysDeptMapper, sysDeptService, deptTreeStrategy,
            deptTreeUtil, deptConverter);
    }

    @Nested
    @DisplayName("部门树查询")
    class ListTreeTest {

        @Test
        @DisplayName("部门树查询，无部门，返回空树")
        void shouldReturnEmpty_whenNoDepts() {
            // Given
            given(sysDeptMapper.selectList(any())).willReturn(List.of());

            // When
            List<DeptTreeVO> tree = service.listTree(DeptTestFactory.treeQueryDTO());

            // Then
            assertThat(tree).isEmpty();
        }

        @Test
        @DisplayName("部门树查询，有部门，按 deptSort 排序")
        void shouldBuildHierarchySortedBySort_whenDeptsExist() {
            // Given：两个顶级组织与一个下级部门
            SysDept rootA = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 2);
            SysDept rootB = DeptTestFactory.rootDept(2L, "ORG002", "华东基地", 2, 1);
            SysDept childDept = DeptTestFactory.dept(3L, "ORG003", "生产部", 1L, 4, 2, "0,1", 1);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(rootA, rootB, childDept));

            // When
            List<DeptTreeVO> tree = service.listTree(DeptTestFactory.treeQueryDTO());

            // Then：根节点按 deptSort 升序，子节点正确挂载
            assertThat(tree).extracting(DeptTreeVO::id).containsExactly(2L, 1L);
            assertThat(tree.get(1).children())
                .extracting(DeptTreeVO::id)
                .containsExactly(3L);
            assertThat(tree.get(0).children()).isNull();
        }

        @Test
        @DisplayName("部门树查询，父级组织不在结果集中（被过滤），其子节点应提升为根避免数据丢失")
        void shouldPromoteOrphanToRoot_whenParentFilteredOut() {
            // Given：父级组织不在结果集中（被过滤），其子节点应提升为根避免数据丢失
            SysDept orphan = DeptTestFactory.dept(5L, "ORG005", "孤儿部门", 99L, 4, 2, "0,1,99", 1);
            SysDept grandChild = DeptTestFactory.dept(6L, "ORG006", "孙级部门", 5L, 5, 3, "0,1,99,5", 1);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(orphan, grandChild));

            // When
            List<DeptTreeVO> tree = service.listTree(DeptTestFactory.treeQueryDTO());

            // Then
            assertThat(tree).extracting(DeptTreeVO::id).containsExactly(5L);
            assertThat(tree.get(0).children())
                .extracting(DeptTreeVO::id)
                .containsExactly(6L);
        }
    }

    @Nested
    @DisplayName("部门创建")
    class CreateTest {

        @Test
        @DisplayName("部门创建，parentId=0 表示顶级，sort/status 为空触发默认值")
        void shouldCreateTopDeptWithDefaults_whenParentIdZero() {
            // Given：parentId=0 表示顶级，sort/status 为空触发默认值
            DeptCreateDTO dto = DeptTestFactory.createDTO("ORG001", "总部", 0L, 1, null, null);
            given(sysDeptMapper.insert(any(SysDept.class))).willAnswer(invocation -> {
                SysDept dept = invocation.getArgument(0, SysDept.class);
                dept.setId(10L);
                dept.setUpdateTime(LocalDateTime.of(2026, 9, 28, 12, 0, 0));
                return 1;
            });

            // When
            var vo = service.create(dto);

            // Then：层级 1、祖级路径 "0"，返回主键与更新时间
            assertThat(vo.id()).isEqualTo(10L);
            assertThat(vo.ancestors()).isEqualTo("0");
            assertThat(vo.deptLevel()).isEqualTo(1);
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 12:00:00");
            assertThat(vo.isDeleted()).isNull();
            verify(sysDeptMapper).insert(deptCaptor.capture());
            SysDept inserted = deptCaptor.getValue();
            assertThat(inserted.getDeptCode()).isEqualTo("ORG001");
            assertThat(inserted.getParentId()).isZero();
            assertThat(inserted.getDeptSort()).isZero();
            assertThat(inserted.getStatus()).isEqualTo(1);
        }

        @Test
        @DisplayName("部门创建，parentId=5L 表示下级部门，计算祖级路径与层级")
        void shouldComputeAncestorsAndLevel_whenCreateChildDept() {
            // Given：父级为 1 级组织（祖级路径 "0"）
            DeptCreateDTO dto = DeptTestFactory.createDTO("ORG003", "生产部", 5L, 4, 1, 1);
            SysDept parent = DeptTestFactory.rootDept(5L, "ORG001", "总部", 1, 1);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(parent));
            given(sysDeptMapper.insert(any(SysDept.class))).willAnswer(invocation -> {
                invocation.getArgument(0, SysDept.class).setId(11L);
                return 1;
            });

            // When
            var vo = service.create(dto);

            // Then：祖级路径 = 父级祖级路径 + 父级 ID
            assertThat(vo.ancestors()).isEqualTo("0,5");
            assertThat(vo.deptLevel()).isEqualTo(2);
        }

        @Test
        @DisplayName("部门创建，组织编码已存在，抛出异常")
        void shouldThrow_whenDeptCodeExists() {
            // Given
            DeptCreateDTO dto = DeptTestFactory.createDTO("ORG001", "总部", 0L, 1, 1, 1);
            willThrow(new BusinessException(SystemErrorCode.DEPT_CODE_EXISTS, "组织编码 ORG001 已存在"))
                .given(sysDeptService).requireDeptCodeAvailable("ORG001");

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_CODE_EXISTS.getCode()))
                .hasMessage("组织编码 ORG001 已存在");
            verify(sysDeptMapper, never()).insert(any(SysDept.class));
        }

        @Test
        @DisplayName("部门创建，父级组织不存在，抛出异常")
        void shouldThrow_whenParentMissing() {
            // Given：父级不在组织表中
            DeptCreateDTO dto = DeptTestFactory.createDTO("ORG003", "生产部", 99L, 4, 1, 1);
            given(sysDeptMapper.selectList(any())).willReturn(List.of());

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()))
                .hasMessage("组织 99 不存在");
            verify(sysDeptMapper, never()).insert(any(SysDept.class));
        }

        @Test
        @DisplayName("部门创建，组织层级超过上限，抛出异常")
        void shouldThrow_whenLevelExceedsFive() {
            // Given：父级已是第 5 级，子级将超过层级上限
            DeptCreateDTO dto = DeptTestFactory.createDTO("ORG006", "车间班组", 5L, 5, 1, 1);
            SysDept parent = DeptTestFactory.dept(5L, "ORG005", "五级车间", 4L, 5, 5, "0,1,2,3,4", 1);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(parent));

            // When + Then
            assertThatThrownBy(() -> service.create(dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_LEVEL_EXCEED.getCode()))
                .hasMessage("组织层级超过上限");
            verify(sysDeptMapper, never()).insert(any(SysDept.class));
        }
    }

    @Nested
    @DisplayName("部门更新")
    class UpdateTest {

        @Test
        @DisplayName("部门更新，parentId 未变化，不触发级联重算")
        void shouldUpdateFields_whenParentUnchanged() {
            // Given：parentId 未变化，不触发级联重算
            SysDept current = DeptTestFactory.rootDept(1L, "ORG001", "旧名称", 1, 1);
            current.setUpdateTime(LocalDateTime.of(2026, 9, 28, 12, 0, 0));
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(0L, "新名称", 4, 2);
            given(sysDeptService.requireDept(1L)).willReturn(current);
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(1);

            // When
            var vo = service.update(1L, dto);

            // Then：层级与祖级路径保持不变
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.ancestors()).isEqualTo("0");
            assertThat(vo.deptLevel()).isEqualTo(1);
            assertThat(vo.updateTime()).isEqualTo("2026-09-28 12:00:00");
            verify(sysDeptMapper).updateById(deptCaptor.capture());
            assertThat(deptCaptor.getValue().getDeptName()).isEqualTo("新名称");
            assertThat(deptCaptor.getValue().getVersion()).isEqualTo(2);
            verify(sysDeptMapper, never()).selectList(any());
        }

        @Test
        @DisplayName("部门更新，parentId 变化，触发级联重算")
        void shouldCascadeRefreshDescendants_whenParentChanged() {
            // Given：B(id2) 从 A(id1) 移动到 C(id3)，B 下有 D(id4)
            SysDept deptA = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            SysDept deptC = DeptTestFactory.rootDept(3L, "ORG003", "华东基地", 2, 2);
            SysDept deptB = DeptTestFactory.dept(2L, "ORG002", "生产部", 1L, 4, 2, "0,1", 1);
            deptB.setUpdateTime(LocalDateTime.of(2026, 9, 28, 12, 0, 0));
            SysDept deptD = DeptTestFactory.dept(4L, "ORG004", "一班", 2L, 5, 3, "0,1,2", 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(3L, "生产部", 4, 5);
            given(sysDeptService.requireDept(2L)).willReturn(deptB);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(deptA, deptB, deptC, deptD));
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(1);

            // When
            var vo = service.update(2L, dto);

            // Then：B 祖级路径重算，D 级联重算
            assertThat(vo.ancestors()).isEqualTo("0,3");
            assertThat(vo.deptLevel()).isEqualTo(2);
            verify(sysDeptMapper, times(2)).updateById(deptCaptor.capture());
            List<SysDept> updated = deptCaptor.getAllValues();
            assertThat(updated.get(0).getId()).isEqualTo(2L);
            assertThat(updated.get(0).getAncestors()).isEqualTo("0,3");
            assertThat(updated.get(0).getParentId()).isEqualTo(3L);
            assertThat(updated.get(1).getId()).isEqualTo(4L);
            assertThat(updated.get(1).getAncestors()).isEqualTo("0,3,2");
            assertThat(updated.get(1).getDeptLevel()).isEqualTo(3);
        }

        @Test
        @DisplayName("部门更新，parentId 变化，新父级是自身，抛出异常")
        void shouldThrow_whenParentIsSelf() {
            // Given
            SysDept current = DeptTestFactory.dept(2L, "ORG002", "生产部", 1L, 4, 2, "0,1", 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(2L, "生产部", 4, 1);
            given(sysDeptService.requireDept(2L)).willReturn(current);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(current));

            // When + Then
            assertThatThrownBy(() -> service.update(2L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("上级组织不可选择自身");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门更新，parentId 变化，新父级是自身下级，抛出异常")
        void shouldThrow_whenParentIsOwnDescendant() {
            // Given：新父级 D(id4) 是自身 A(id1) 的下级
            SysDept deptA = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            SysDept deptB = DeptTestFactory.dept(2L, "ORG002", "生产部", 1L, 4, 2, "0,1", 1);
            SysDept deptD = DeptTestFactory.dept(4L, "ORG004", "一班", 2L, 5, 3, "0,1,2", 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(4L, "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(deptA);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(deptA, deptB, deptD));

            // When + Then
            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("上级组织不可选择自身或自身下级组织");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门更新，parentId 变化，新父级不在组织表中，抛出异常")
        void shouldThrow_whenNewParentMissing() {
            // Given：新父级不在组织表中
            SysDept current = DeptTestFactory.dept(2L, "ORG002", "生产部", 1L, 4, 2, "0,1", 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(99L, "生产部", 4, 1);
            given(sysDeptService.requireDept(2L)).willReturn(current);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(current));

            // When + Then
            assertThatThrownBy(() -> service.update(2L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()))
                .hasMessage("组织 99 不存在");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门更新，parentId 变化，新父级已是第 5 级，抛出异常")
        void shouldThrow_whenNewLevelExceedsFive() {
            // Given：新父级已是第 5 级
            SysDept current = DeptTestFactory.dept(2L, "ORG002", "生产部", 1L, 4, 2, "0,1", 1);
            SysDept deepParent = DeptTestFactory.dept(5L, "ORG005", "五级车间", 4L, 5, 5, "0,1,2,3,4", 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(5L, "生产部", 4, 1);
            given(sysDeptService.requireDept(2L)).willReturn(current);
            given(sysDeptMapper.selectList(any())).willReturn(List.of(current, deepParent));

            // When + Then
            assertThatThrownBy(() -> service.update(2L, dto))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_LEVEL_EXCEED.getCode()))
                .hasMessage("组织层级超过上限");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门更新，parentId 变化，新父级层级超过上限，抛出异常")
        void shouldThrow_whenOptimisticLockConflict() {
            // Given
            SysDept current = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            DeptUpdateDTO dto = DeptTestFactory.updateDTO(0L, "总部", 1, 9);
            given(sysDeptService.requireDept(1L)).willReturn(current);
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.update(1L, dto))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("部门删除")
    class DeleteTest {

        @Test
        @DisplayName("部门删除，无下级无用户，成功删除")
        void shouldDelete_whenNoChildrenAndNoUsers() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            given(sysDeptMapper.deleteById(1L)).willReturn(1);

            // When
            var vo = service.delete(1L);

            // Then
            assertThat(vo.id()).isEqualTo(1L);
            assertThat(vo.isDeleted()).isEqualTo(1);
            assertThat(vo.ancestors()).isNull();
            verify(sysDeptMapper).deleteById(1L);
        }

        @Test
        @DisplayName("部门删除，有下级组织，抛出异常")
        void shouldThrow_whenHasChildren() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            willThrow(new BusinessException(SystemErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 总部 存在下级组织或绑定用户，不可删除"))
                .given(sysDeptService).requireDeptHasNoChildren(dept);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_HAS_CHILDREN_OR_USERS.getCode()))
                .hasMessage("组织 总部 存在下级组织或绑定用户，不可删除");
            verify(sysDeptMapper, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("部门删除，无下级但绑定了用户，抛出异常")
        void shouldThrow_whenHasUsers() {
            // Given：无下级但绑定了用户
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            willThrow(new BusinessException(SystemErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 总部 存在下级组织或绑定用户，不可删除"))
                .given(sysDeptService).requireDeptHasNoUsers(dept);

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_HAS_CHILDREN_OR_USERS.getCode()))
                .hasMessage("组织 总部 存在下级组织或绑定用户，不可删除");
            verify(sysDeptMapper, never()).deleteById(anyLong());
        }
        @Test
        @DisplayName("部门删除，部门不存在，抛出异常")
        void shouldThrow_whenDeptNotFound() {
            // Given
            given(sysDeptService.requireDept(1L))
                .willThrow(new BusinessException(SystemErrorCode.DEPT_NOT_FOUND, "组织 1 不存在"));

            // When + Then
            assertThatThrownBy(() -> service.delete(1L))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()))
                .hasMessage("组织 1 不存在");
            verify(sysDeptMapper, never()).deleteById(anyLong());
        }
    }

    @Nested
    @DisplayName("部门状态变更")
    class ChangeStatusTest {

        @Test
        @DisplayName("部门状态变更，成功启用，仅更新状态与乐观锁版本")
        void shouldEnableDept_whenStatusOne() {
            // Given：组织存在且当前停用
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            dept.setStatus(0);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(1);

            // When
            var vo = service.changeStatus(1L, DeptTestFactory.statusDTO(1, 2));

            // Then：patch 实体仅承载 id/status/version
            assertThat(vo.status()).isEqualTo(1);
            verify(sysDeptMapper).updateById(deptCaptor.capture());
            SysDept patch = deptCaptor.getValue();
            assertThat(patch.getId()).isEqualTo(1L);
            assertThat(patch.getStatus()).isEqualTo(1);
            assertThat(patch.getVersion()).isEqualTo(2);
            assertThat(patch.getDeptCode()).isNull();
        }

        @Test
        @DisplayName("部门状态变更，成功停用")
        void shouldDisableDept_whenStatusZero() {
            // Given：组织存在且当前启用
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(1);

            // When
            var vo = service.changeStatus(1L, DeptTestFactory.statusDTO(0, 3));

            // Then
            assertThat(vo.status()).isZero();
            verify(sysDeptMapper).updateById(deptCaptor.capture());
            assertThat(deptCaptor.getValue().getStatus()).isZero();
            assertThat(deptCaptor.getValue().getVersion()).isEqualTo(3);
        }

        @Test
        @DisplayName("部门状态变更，状态值非法，抛出 10001")
        void shouldThrow10001_whenStatusValueIllegal() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);

            // When + Then
            assertThatThrownBy(() -> service.changeStatus(1L, DeptTestFactory.statusDTO(2, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("状态取值非法，仅支持 0-停用 1-启用");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门状态变更，组织不存在，抛出 18024")
        void shouldThrow18024_whenDeptNotFound() {
            // Given
            given(sysDeptService.requireDept(99L))
                .willThrow(new BusinessException(SystemErrorCode.DEPT_NOT_FOUND, "组织 99 不存在"));

            // When + Then
            assertThatThrownBy(() -> service.changeStatus(99L, DeptTestFactory.statusDTO(1, 2)))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(SystemErrorCode.DEPT_NOT_FOUND.getCode()))
                .hasMessage("组织 99 不存在");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("部门状态变更，乐观锁冲突（更新 0 行），抛出 10601")
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            SysDept dept = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            given(sysDeptService.requireDept(1L)).willReturn(dept);
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.changeStatus(1L, DeptTestFactory.statusDTO(1, 9)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }

    @Nested
    @DisplayName("move：组织上移/下移")
    class MoveTest {

        @Test
        @DisplayName("上移时与前一节点交换并规范化重排，仅变更行落库")
        void shouldSwapWithNeighborAndNormalize_whenMoveUp() {
            // Given：顶级三个组织 sort=10/20/30，目标为第二个
            SysDept first = DeptTestFactory.rootDept(1L, "D001", "总部", 1, 10);
            SysDept target = DeptTestFactory.rootDept(2L, "D002", "华东分部", 1, 20);
            SysDept third = DeptTestFactory.rootDept(3L, "D003", "华南分部", 1, 30);
            given(sysDeptService.requireDept(2L)).willReturn(target);
            given(sysDeptService.listSiblings(0L)).willReturn(List.of(first, target, third));
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(1);

            // When
            service.move(2L, new DeptMoveDTO("up"));

            // Then：交换后 target=10、first=20，third 不变；仅两行落库
            assertThat(target.getDeptSort()).isEqualTo(10);
            assertThat(first.getDeptSort()).isEqualTo(20);
            assertThat(third.getDeptSort()).isEqualTo(30);
            verify(sysDeptMapper, times(2)).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("已处于同级底部时下移抛 10001")
        void shouldThrow10001_whenMoveDownAtBottom() {
            // Given：目标为同级最后一行
            SysDept first = DeptTestFactory.rootDept(1L, "D001", "总部", 1, 10);
            SysDept target = DeptTestFactory.rootDept(2L, "D002", "华东分部", 1, 20);
            given(sysDeptService.requireDept(2L)).willReturn(target);
            given(sysDeptService.listSiblings(0L)).willReturn(List.of(first, target));

            // When + Then
            assertThatThrownBy(() -> service.move(2L, new DeptMoveDTO("down")))
                .isInstanceOfSatisfying(BusinessException.class,
                    ex -> assertThat(ex.getCode()).isEqualTo(CommonErrorCode.PARAM_ERROR.getCode()))
                .hasMessage("已处于同级底部，无法下移");
            verify(sysDeptMapper, never()).updateById(any(SysDept.class));
        }

        @Test
        @DisplayName("乐观锁冲突（更新 0 行），抛出 10601")
        void shouldThrow10601_whenOptimisticLockConflict() {
            // Given
            SysDept target = DeptTestFactory.rootDept(1L, "D001", "总部", 1, 10);
            SysDept second = DeptTestFactory.rootDept(2L, "D002", "华东分部", 1, 20);
            given(sysDeptService.requireDept(1L)).willReturn(target);
            given(sysDeptService.listSiblings(0L)).willReturn(List.of(target, second));
            given(sysDeptMapper.updateById(any(SysDept.class))).willReturn(0);

            // When + Then
            assertThatThrownBy(() -> service.move(1L, new DeptMoveDTO("down")))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessage("数据已被其他操作修改，请重试");
        }
    }
}
