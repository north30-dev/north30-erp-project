package me.north30.erp.system.dept.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.system.common.util.DeptTreeUtil;
import me.north30.erp.system.common.util.SiblingSortUtil;
import me.north30.erp.system.dept.converter.DeptConverter;
import me.north30.erp.system.dept.dto.DeptCreateDTO;
import me.north30.erp.system.dept.dto.DeptMoveDTO;
import me.north30.erp.system.dept.dto.DeptStatusDTO;
import me.north30.erp.system.dept.dto.DeptTreeQueryDTO;
import me.north30.erp.system.dept.dto.DeptUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptApplicationService;
import me.north30.erp.system.dept.service.SysDeptDomainService;
import me.north30.erp.system.dept.strategy.DeptTreeStrategy;
import me.north30.erp.system.dept.vo.DeptMutationVO;
import me.north30.erp.system.dept.vo.DeptStatusVO;
import me.north30.erp.system.dept.vo.DeptTreeVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

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
        // 新建实体 id 为 null，自引用/环检测天然不命中，仅父级存在性生效；顶级（0/null）不查全量
        if (dto.parentId() != null && dto.parentId() != 0L) {
            dept.applyParent(deptTreeStrategy.requireValidNewParent(
                deptTreeStrategy.loadDeptMap(), dept, dto.parentId()));
        } else {
            dept.applyParent(null);
        }
        dept.applyCreateDefault();
        // 未显式指定显示顺序时取同级 max(deptSort)+10（08 文档阶段四排序统一）
        if (dto.deptSort() == null) {
            dept.setDeptSort(sysDeptService.nextDeptSort(dto.parentId()));
        }
        sysDeptMapper.insert(dept);
        return new DeptMutationVO(dept.getId(), dept.getAncestors(), dept.getDeptLevel(),
            DateTimeFormatUtil.format(dept.getUpdateTime()), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptMutationVO update(Long id, DeptUpdateDTO dto) {
        SysDept dept = sysDeptService.requireDept(id);
        boolean parentChanged = dept.parentChangedTo(dto.parentId());
        // 一次查全量贯穿换父校验与级联重算（禁 N+1）
        Map<Long, SysDept> deptMap = null;
        if (parentChanged) {
            deptMap = deptTreeStrategy.loadDeptMap();
            dept = deptMap.get(id);
            dept.applyParent(deptTreeStrategy.requireValidNewParent(deptMap, dept, dto.parentId()));
        }

        deptConverter.updateDept(dto, dept);
        dept.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDeptMapper.updateById(dept));

        if (parentChanged) {
            deptTreeStrategy.cascadeRefreshDescendants(deptMap, dept);
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeptStatusVO changeStatus(Long id, DeptStatusDTO dto) {
        SysDept dept = sysDeptService.requireDept(id);
        // 状态值域守卫（10001）收敛于实体 changeStatus，组织无内置保护
        dept.changeStatus(dto.status());
        SysDept patch = new SysDept();
        patch.setId(id);
        patch.setStatus(dept.getStatus());
        patch.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysDeptMapper.updateById(patch));
        return new DeptStatusVO(dept.getStatus());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, DeptMoveDTO dto) {
        SysDept dept = sysDeptService.requireDept(id);
        // 同级按 sort ASC, id ASC 一次查出，规范化重排 + 相邻交换，变更行逐一乐观锁落库
        List<SysDept> siblings = sysDeptService.listSiblings(dept.getParentId());
        List<SysDept> changed = SiblingSortUtil.moveSibling(siblings, dept,
            "up".equals(dto.direction()), SysDept::getId, SysDept::getDeptSort, SysDept::setDeptSort);
        for (SysDept row : changed) {
            UpdateGuardUtil.checkOptimisticLockConflict(sysDeptMapper.updateById(row));
        }
    }

}
