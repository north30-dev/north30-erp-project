package me.north30.erp.system.role.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.role.service.SysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 用户-角色关联服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl implements SysUserRoleService {

    private final SysUserRoleMapper sysUserRoleMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SysUserRole> listByUserId(Long userId) {
        return sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUserRole(SysUserRole userRole) {
        return sysUserRoleMapper.insert(userRole) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysUserRole> listByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, roleIds));
    }

    @Override
    @Transactional(readOnly = true)
    public long countUsersByRoleId(Long roleId) {
        Long count = sysUserRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        return count == null ? 0L : count;
    }
}
