package me.north30.erp.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.util.DeptTreeUtil;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptManageService;
import me.north30.erp.system.dept.strategy.DeptTreeStrategy;
import me.north30.erp.system.dept.vo.DeptMutationVO;
import me.north30.erp.system.dept.vo.DeptTreeVO;
import me.north30.erp.system.user.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 组织/部门管理服务实现。
 * <p>树查询一次查全量内存组树；修改父级时环检测 + 级联重算下级祖级路径与层级（{@link DeptTreeStrategy}）；
 * 删除前校验下级组织与挂靠用户（sys_user.dept_id）。层级上限与祖级路径由 {@link SysDept#applyParent} 承载。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysDeptManageServiceImpl implements SysDeptManageService {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 默认启用状态 */
    private static final int STATUS_ENABLED = 1;

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDeptMapper sysDeptMapper;
    private final SysUserService sysUserService;
    private final DeptTreeStrategy deptTreeStrategy;
    private final DeptTreeUtil deptTreeUtil;
    private final DeptConverter deptConverter;

    @Override
    @Transactional(readOnly = true)
    public List<DeptTreeVO> listTree(DeptTreeQueryDTO query) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<SysDept>()
            .like(StringUtils.hasText(query.deptName()), SysDept::getDeptName, query.deptName())
            .eq(query.deptType() != null, SysDept::getDeptType, query.deptType())
            .eq(query.status() != null, SysDept::getStatus, query.status())
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
        SysDept dept = deptConverter.toCreatedEntity(dto);
        dept.applyParent(deptTreeStrategy.resolveParent(dto.parentId()));
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
        SysDept dept = current;
        if (parentChanged) {
            if (Objects.equals(dto.parentId(), id)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身");
            }
            // 一次查全量用于父级解析、环检测与级联重算（禁 N+1）
            Map<Long, SysDept> deptMap = deptTreeStrategy.loadDeptMap();
            dept = deptMap.get(id);
            SysDept newParent = null;
            if (dto.parentId() != null && dto.parentId() != ROOT_PARENT_ID) {
                newParent = deptTreeStrategy.requireParentInMap(deptMap, dto.parentId());
                if (deptTreeStrategy.inAncestorChain(deptMap, id, newParent.getId())) {
                    throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身或自身下级组织");
                }
            }
            dept.applyParent(newParent);
        }

        deptConverter.toUpdatedEntity(dto, dept);

        if (sysDeptMapper.updateById(dept) == 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        if (parentChanged) {
            deptTreeStrategy.cascadeRefreshDescendants(deptTreeStrategy.loadDeptMap(), dept);
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
        if (deptTreeStrategy.countChildren(id) > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 存在下级组织，不可删除");
        }
        if (sysUserService.countByDeptId(id) > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 已绑定用户，不可删除");
        }
        sysDeptMapper.deleteById(id);
        return new DeptMutationVO(id, null, null, null, DELETED);
    }
}
