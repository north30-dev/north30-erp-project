package me.north30.erp.system.dept.service.impl;

import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.dept.service.SysDeptService;
import me.north30.erp.system.user.service.SysUserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

/**
 * 组织/部门服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl implements SysDeptService {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserService sysUserService;

    @Override
    @Transactional(readOnly = true)
    public SysDept getById(Long id) {
        return sysDeptMapper.selectById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SysDept requireDept(Long id) {
        SysDept dept = sysDeptMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + id + " 不存在");
        }
        return dept;
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDeptCodeAvailable(String deptCode) {
        Long exists = sysDeptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getDeptCode, deptCode));
        if (exists != null && exists > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_CODE_EXISTS, "组织编码 " + deptCode + " 已存在");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDeptHasNoChildren(SysDept dept) {
        Long count = sysDeptMapper.selectCount(
            new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, dept.getId()));
        if (count != null && count > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 存在下级组织或绑定用户，不可删除");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void requireDeptHasNoUsers(SysDept dept) {
        if (sysUserService.countByDeptId(dept.getId()) > 0) {
            throw new BusinessException(SystemManageErrorCode.DEPT_HAS_CHILDREN_OR_USERS,
                "组织 " + dept.getDeptName() + " 存在下级组织或绑定用户，不可删除");
        }
    }
}
