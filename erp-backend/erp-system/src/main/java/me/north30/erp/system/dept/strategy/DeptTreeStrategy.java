package me.north30.erp.system.dept.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptService;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织树结构策略：父级解析、环检测与下级级联重算。
 * <p>层级上限与祖级路径计算由 {@link SysDept#applyParent(SysDept)} 充血承载。</p>
 */
@Component
@RequiredArgsConstructor
public class DeptTreeStrategy {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    private final SysDeptMapper sysDeptMapper;
    private final SysDeptService sysDeptService;

    /**
     * 解析父级组织：parentId 为空或 0 表示顶级，返回 null；否则必须存在。
     * 
     * @param parentId 父组织 ID
     * @return 父组织实体
     */
    public SysDept resolveParent(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        return sysDeptService.requireDept(parentId);
    }

    /**
     * 环检测：从 startId 沿父级链向上走，若经过 selfId 说明 startId 是自身的后代。
     * 
     * @param deptMap 组织索引（按 ID 建立）
     * @param selfId  当前组织 ID
     * @param startId 开始组织 ID
     * @return 是否是后代
     */
    public boolean inAncestorChain(Map<Long, SysDept> deptMap, Long selfId, Long startId) {
        Long cursor = startId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null && cursor != ROOT_PARENT_ID && visited.add(cursor)) {
            if (cursor.equals(selfId)) {
                return true;
            }
            SysDept node = deptMap.get(cursor);
            cursor = node == null ? null : node.getParentId();
        }
        return false;
    }

    /**
     * 级联重算下级组织的祖级路径与层级（BFS，父节点先于子节点刷新）。
     * 
     * @param deptMap 组织索引（按 ID 建立）
     * @param updated  更新的组织实体
     */
    public void cascadeRefreshDescendants(Map<Long, SysDept> deptMap, SysDept updated) {
        Map<Long, List<SysDept>> childrenMap = deptMap.values().stream()
            .collect(Collectors.groupingBy(SysDept::getParentId));
        Deque<SysDept> queue = new ArrayDeque<>(childrenMap.getOrDefault(updated.getId(), List.of()));
        while (!queue.isEmpty()) {
            SysDept child = queue.poll();
            SysDept parent = deptMap.get(child.getParentId());
            child.applyParent(parent);
            sysDeptMapper.updateById(child);
            queue.addAll(childrenMap.getOrDefault(child.getId(), List.of()));
        }
    }

    /**
     * 一次查全量组织并按 ID 建索引（用于父级解析、环检测与级联重算，禁 N+1）。
     * 
     * @return 组织索引（按 ID 建立）
     */
    public Map<Long, SysDept> loadDeptMap() {
        return sysDeptMapper.selectList(null).stream()
            .collect(Collectors.toMap(SysDept::getId, Function.identity()));
    }

    /**
     * 校验指定父级存在于全量组织中，不存在抛 18022。
     * 
     * @param deptMap 组织索引（按 ID 建立）
     * @param parentId 父组织 ID
     * @return 父组织实体
     */
    public SysDept requireParentInMap(Map<Long, SysDept> deptMap, Long parentId) {
        SysDept parent = deptMap.get(parentId);
        if (parent == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + parentId + " 不存在");
        }
        return parent;
    }

    /**
     * 统计指定组织的下级组织数（逻辑删除过滤）。
     * 
     * @param deptId 组织 ID
     * @param deptMap 组织索引（按 ID 建立）
     * @return 下级组织数
     */
    public long countChildren(Long deptId) {
        Long count = sysDeptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, deptId));
        return count == null ? 0L : count;
    }
}
