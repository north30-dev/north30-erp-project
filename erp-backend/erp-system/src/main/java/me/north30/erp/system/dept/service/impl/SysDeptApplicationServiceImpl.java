package me.north30.erp.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.system.common.util.DeptTreeUtil;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptApplicationService;
import me.north30.erp.system.dept.service.SysDeptDomainService;
import me.north30.erp.system.dept.strategy.DeptTreeStrategy;
import me.north30.erp.system.dept.vo.DeptMutationVO;
import me.north30.erp.system.dept.vo.DeptTreeVO;
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
public class SysDeptApplicationServiceImpl implements SysDeptApplicationService {

    /** 逻辑删除标记 */
    private static final int DELETED = 1;

    private final SysDeptMapper sysDeptMapper;
    private final SysDeptDomainService sysDeptService;
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
        sysDeptService.requireDeptCodeAvailable(dto.deptCode());
        SysDept dept = deptConverter.createDept(dto);
        dept.applyParent(deptTreeStrategy.resolveParent(dto.parentId()));
        dept.applyCreateDefault();
        sysDeptMapper.insert(dept);
        return new DeptMutationVO(dept.getId(), dept.getAncestors(), dept.getDeptLevel(),
            DateTimeFormatUtil.format(dept.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO update(Long id, DeptUpdateDTO dto) {
        SysDept dept = sysDeptService.requireDept(id);
        boolean parentChanged = dept.parentChangedTo(dto.parentId());
    
        if (parentChanged) {
            if (Objects.equals(dto.parentId(), id)) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身");
            }
            // 一次查全量用于父级解析、环检测与级联重算（禁 N+1）
            Map<Long, SysDept> deptMap = deptTreeStrategy.loadDeptMap();
            dept = deptMap.get(id);
            SysDept newParent = null;
            // 新父级为顶级（0）时无父级可解析；否则解析父级并做环检测
            if (dto.parentId() != null && dto.parentId() != 0L) {
                newParent = deptTreeStrategy.requireParentInMap(deptMap, dto.parentId());
                if (deptTreeStrategy.inAncestorChain(deptMap, id, newParent.getId())) {
                    throw new BusinessException(CommonErrorCode.PARAM_ERROR, "上级组织不可选择自身或自身下级组织");
                }
            }
            dept.applyParent(newParent);
        }

        deptConverter.updateDept(dto, dept);
        dept.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDeptMapper.updateById(dept));
        
        if (parentChanged) {
            deptTreeStrategy.cascadeRefreshDescendants(deptTreeStrategy.loadDeptMap(), dept);
        }
        return new DeptMutationVO(dept.getId(), dept.getAncestors(), dept.getDeptLevel(),
            DateTimeFormatUtil.format(dept.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO delete(Long id) {
        SysDept dept = sysDeptService.requireDept(id);
        sysDeptService.requireDeptHasNoChildren(dept);
        sysDeptService.requireDeptHasNoUsers(dept);
        sysDeptMapper.deleteById(id);
        return new DeptMutationVO(id, null, null, null, DELETED);
    }
}
