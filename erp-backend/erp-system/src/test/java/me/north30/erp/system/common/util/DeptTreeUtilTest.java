package me.north30.erp.system.common.util;

import me.north30.erp.system.dept.DeptTestFactory;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.vo.DeptTreeVO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link DeptTreeUtil} 纯工具类测试：平铺列表组树、孤儿节点提升与 VO 转换。
 */
class DeptTreeUtilTest {

    @Nested
    @DisplayName("buildTree：构建部门树")
    class BuildTreeTest {

        @Test
        @DisplayName("输入 null 时返回空树")
        void shouldReturnEmpty_whenInputNull() {
            // When + Then
            assertThat(DeptTreeUtil.buildTree(null)).isEmpty();
        }

        @Test
        @DisplayName("输入空列表时返回空树")
        void shouldReturnEmpty_whenInputEmpty() {
            // When + Then
            assertThat(DeptTreeUtil.buildTree(List.of())).isEmpty();
        }

        @Test
        @DisplayName("输入平铺列表时返回排序后的树")
        void shouldBuildHierarchySortedBySort_whenFlatListGiven() {
            // Given：三级结构，同级按 deptSort 升序，null 排序值排最后
            SysDept rootA = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 2);
            SysDept rootB = DeptTestFactory.rootDept(2L, "ORG002", "华东基地", 2, 1);
            SysDept rootNoSort = DeptTestFactory.rootDept(3L, "ORG003", "华南基地", 3, null);
            SysDept childDept = DeptTestFactory.dept(4L, "ORG004", "生产部", 1L, 4, 2, "0,1", 1);

            // When
            List<DeptTreeVO> tree = DeptTreeUtil.buildTree(List.of(rootA, rootB, rootNoSort, childDept));

            // Then
            assertThat(tree).extracting(DeptTreeVO::getId).containsExactly(2L, 1L, 3L);
            assertThat(tree.get(1).getChildren())
                .extracting(DeptTreeVO::getId)
                .containsExactly(4L);
            assertThat(tree.get(0).getChildren()).isNull();
        }

        @Test
        @DisplayName("输入平铺列表时，带下级的孤儿节点提升提升为根")
        void shouldPromoteOrphanToRoot_whenParentMissing() {
            // Given：父节点被过滤后，带下级的孤儿节点应提升为根，避免子树静默丢失
            SysDept orphan = DeptTestFactory.dept(5L, "ORG005", "孤儿组织", 99L, 4, 2, "0,1,99", 1);
            SysDept grandChild = DeptTestFactory.dept(6L, "ORG006", "下级组织", 5L, 5, 3, "0,1,99,5", 1);

            // When
            List<DeptTreeVO> tree = DeptTreeUtil.buildTree(List.of(orphan, grandChild));

            // Then
            assertThat(tree).extracting(DeptTreeVO::getId).containsExactly(5L);
            assertThat(tree.get(0).getChildren())
                .extracting(DeptTreeVO::getId)
                .containsExactly(6L);
        }

        @Test
        @DisplayName("输入平铺列表时，孤儿节点没有子节点时被丢弃")
        void shouldDropOrphanLeaf_whenParentMissing() {
            // Given：孤儿节点没有子节点时按现有实现被丢弃（分支行为以源码为准）
            SysDept root = DeptTestFactory.rootDept(1L, "ORG001", "总部", 1, 1);
            SysDept orphanLeaf = DeptTestFactory.dept(9L, "ORG009", "孤儿叶子", 99L, 4, 2, "0,1,99", 1);

            // When
            List<DeptTreeVO> tree = DeptTreeUtil.buildTree(List.of(root, orphanLeaf));

            // Then
            assertThat(tree).extracting(DeptTreeVO::getId).containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("toVO：将部门实体转换为部门树节点")
    class ToVOTest {

        @Test
        @DisplayName("将部门实体转换为部门树节点，字段一一对应")
        void shouldMapEntityFieldsToVO() {
            // Given
            SysDept dept = DeptTestFactory.dept(1L, "ORG001", "总部", 0L, 1, 1, "0", 2);
            dept.setLeader("张三");
            dept.setPhone("13800000000");

            // When
            DeptTreeVO vo = DeptTreeUtil.toVO(dept);

            // Then：字段一一对应
            assertThat(vo.getId()).isEqualTo(1L);
            assertThat(vo.getDeptCode()).isEqualTo("ORG001");
            assertThat(vo.getDeptName()).isEqualTo("总部");
            assertThat(vo.getParentId()).isZero();
            assertThat(vo.getDeptType()).isEqualTo(1);
            assertThat(vo.getDeptLevel()).isEqualTo(1);
            assertThat(vo.getAncestors()).isEqualTo("0");
            assertThat(vo.getLeader()).isEqualTo("张三");
            assertThat(vo.getPhone()).isEqualTo("13800000000");
            assertThat(vo.getDeptSort()).isEqualTo(2);
            assertThat(vo.getStatus()).isEqualTo(1);
        }
    }
}
