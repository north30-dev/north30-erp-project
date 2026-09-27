package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysUserRole;
import me.north30.erp.system.core.mapper.SysUserRoleMapper;
import me.north30.erp.system.core.service.ISysUserRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 用户-角色关联服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysUserRoleServiceImpl implements ISysUserRoleService {

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
}
