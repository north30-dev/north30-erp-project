package me.north30.erp.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.dept.service.SysDeptManageService;
import me.north30.erp.system.common.util.DateTimeFormatUtil;
import me.north30.erp.system.common.util.DeptTreeUtil;
import me.north30.erp.system.dept.vo.DeptMutationVO;
import me.north30.erp.system.dept.vo.DeptTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 组织/部门管理服务实现。
 * <p>树查询一次查全量内存组树；修改父级时环检测 + 级联重算下级祖级路径与层级；
 * 删除前校验下级组织与挂靠用户（sys_user.dept_id）。</p>
 */
@Service
@RequiredArgsConstructor
public class SysDeptManageServiceImpl implements SysDeptManageService {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 组织层级上限（接口文档 18027） */
    private static final int MAX_DEPT_LEVEL = 5;

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final DeptTreeUtil deptTreeUtil;
    private final DeptConverter deptConverter;

    @Override
    @Transactional(readOnly = true)
    public List<DeptTreeVO> listTree(DeptTreeQueryDTO query) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
            .like(StringUtils.hasText(query.getDeptName()), SysDept::getDeptName, query.getDeptName())
            .eq(query.getDeptType() != null, SysDept::getDeptType, query.getDeptType())
            .eq(query.getStatus() != null, SysDept::getStatus, query.getStatus())
            .orderByAsc(SysDept::getDeptSort)
            .orderByAsc(SysDept::getId);
        return deptTreeUtil.buildTree(sysDeptMapper.selectList(wrapper));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO create(DeptCreateDTO dto) {
        Long exists = sysDeptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptCode, dto.deptCode()));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_CODE_EXISTS, "组织编码 " + dto.deptCode() + " 已存在");
        }
        SysDept parent = resolveParent(dto.parentId());
        int deptLevel = parent == null ? 1 : parent.getDeptLevel() + 1;
        if (deptLevel > MAX_DEPT_LEVEL) {
            throw new BusinessException(SystemManageErrorCode.DEPT_LEVEL_EXCEED);
        }
        SysDept dept = deptConverter.toEntity(dto);
        dept.setDeptLevel(deptLevel);
        dept.setAncestors(buildAncestors(parent));
        if (dept.getDeptSort() == null) {
            dept.setDeptSort(0);
        }
        if (dept.getStatus() == null) {
            dept.setStatus(STATUS_ENABLED);
        }
        sysDeptMapper.insert(dept);
        return new DeptMutationVO(dept.getId(), dept.getAncestors(), dept.getDeptLevel(),
            DateTimeFormatUtil.format(dept.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO update(Long id, DeptUpdateDTO dto) {
        SysDept current = sysDeptMapper.selectById(id);
        if (current == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + id + " 不存在");
        }
        boolean parentChanged = !Objects.equals(current.getParentId(), dto.parentId());
        Map<Long, SysDept> deptMap = Map.of();
        SysDept dept = current;
        if (parentChanged) {
            // 一次查全量用于父级解析、环检测与级联重算（禁 N+1）
            deptMap = sysDeptMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysDept::getId, Function.identity()));
            dept = deptMap.get(id);
            if (Objects.equals(dto.parentId(), id)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身");
            }
            SysDept newParent = null;
            if (dto.parentId() != null && dto.parentId() != ROOT_PARENT_ID) {
                newParent = deptMap.get(dto.parentId());
                if (newParent == null) {
                    throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + dto.parentId() + " 不存在");
                }
                if (inAncestorChain(deptMap, id, newParent.getId())) {
                    throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身或自身下级组织");
                }
            }
            dept.setParentId(dto.parentId());
            dept.setDeptLevel(newParent == null ? 1 : newParent.getDeptLevel() + 1);
            if (dept.getDeptLevel() > MAX_DEPT_LEVEL) {
                throw new BusinessException(SystemManageErrorCode.DEPT_LEVEL_EXCEED);
            }
            dept.setAncestors(buildAncestors(newParent));
        }
        dept.setDeptName(dto.deptName());
        dept.setDeptType(dto.deptType());
        if (dto.leader() != null) {
            dept.setLeader(dto.leader());
        }
        if (dto.phone() != null) {
            dept.setPhone(dto.phone());
        }
        if (dto.deptSort() != null) {
            dept.setDeptSort(dto.deptSort());
        }
        if (dto.status() != null) {
            dept.setStatus(dto.status());
        }
        if (dto.remark() != null) {
            dept.setRemark(dto.remark());
        }
        dept.setVersion(dto.version());
        if (sysDeptMapper.updateById(dept) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        if (parentChanged) {
            cascadeRefreshDescendants(deptMap, dept);
        }
        return new DeptMutationVO(dept.getId(), dept.getAncestors(), dept.getDeptLevel(),
            DateTimeFormatUtil.format(dept.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO delete(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + id + " 不存在");
        }
        Long childCount = sysDeptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 存在下级组织，不可删除");
        }
        Long userCount = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        if (userCount != null && userCount > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 已绑定用户，不可删除");
        }
        sysDeptMapper.deleteById(id);
        return new DeptMutationVO(id, null, null, null, DELETED);
    }

    /**
     * 解析父级组织：parentId 为空或 0 表示顶级，返回 null；否则必须存在。
     */
    private SysDept resolveParent(Long parentId) {
        if (parentId == null || parentId == ROOT_PARENT_ID) {
            return null;
        }
        SysDept parent = sysDeptMapper.selectById(parentId);
        if (parent == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + parentId + " 不存在");
        }
        return parent;
    }

    /**
     * 计算祖级路径：顶级为 "0"，否则 父级祖级路径 + 父级 ID（如 0,1,5）。
     */
    private String buildAncestors(SysDept parent) {
        return parent == null ? "0" : parent.getAncestors() + "," + parent.getId();
    }

    /**
     * 环检测：从 startId 沿父级链向上走，若经过 selfId 说明 startId 是自身的后代。
     */
    private boolean inAncestorChain(Map<Long, SysDept> deptMap, Long selfId, Long startId) {
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
     */
    private void cascadeRefreshDescendants(Map<Long, SysDept> deptMap, SysDept updated) {
        Map<Long, List<SysDept>> childrenMap = deptMap.values().stream()
            .collect(Collectors.groupingBy(SysDept::getParentId));
        Deque<SysDept> queue = new ArrayDeque<>(childrenMap.getOrDefault(updated.getId(), List.of()));
        while (!queue.isEmpty()) {
            SysDept child = queue.poll();
            SysDept parent = deptMap.get(child.getParentId());
            child.setAncestors(parent.getAncestors() + "," + parent.getId());
            child.setDeptLevel(parent.getDeptLevel() + 1);
            if (child.getDeptLevel() > MAX_DEPT_LEVEL) {
                throw new BusinessException(SystemManageErrorCode.DEPT_LEVEL_EXCEED);
            }
            sysDeptMapper.updateById(child);
            queue.addAll(childrenMap.getOrDefault(child.getId(), List.of()));
        }
    }
}
