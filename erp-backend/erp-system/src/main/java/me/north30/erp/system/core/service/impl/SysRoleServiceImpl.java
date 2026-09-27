package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysRole;
import me.north30.erp.system.core.mapper.SysRoleMapper;
import me.north30.erp.system.core.service.ISysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;

/**
 * 角色服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements ISysRoleService {

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
        return sysRoleMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRole(SysRole role) {
        return sysRoleMapper.insert(role) > 0;
    }
}
