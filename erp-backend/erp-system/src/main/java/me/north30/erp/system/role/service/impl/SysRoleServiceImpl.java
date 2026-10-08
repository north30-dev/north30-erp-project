package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 角色服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private final SysRoleMapper sysRoleMapper;

    @Override
    @Transactional(readOnly = true)
    public SysRole getById(Long id) {
        return sysRoleMapper.selectById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public SysRole getByRoleCode(String roleCode) {
        return sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysRole> listByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(SysRole role) {
        return sysRoleMapper.insert(role) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public SysRole requireRole(Long id) {
        SysRole role = sysRoleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND, "角色 " + id + " 不存在");
        }
        return role;
    }
}
