package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysRoleMenu;
import me.north30.erp.system.core.mapper.SysRoleMenuMapper;
import me.north30.erp.system.core.service.SysRoleMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 角色-菜单关联服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysRoleMenuServiceImpl implements SysRoleMenuService {

    private final SysRoleMenuMapper sysRoleMenuMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SysRoleMenu> listByRoleIds(Collection<Long> roleIds) {
        if (CollectionUtils.isEmpty(roleIds)) {
            return List.of();
        }
        return sysRoleMenuMapper.selectList(
            new LambdaQueryWrapper<SysRoleMenu>().in(SysRoleMenu::getRoleId, roleIds));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createRoleMenu(SysRoleMenu roleMenu) {
        return sysRoleMenuMapper.insert(roleMenu) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createBatch(List<SysRoleMenu> roleMenus) {
        if (CollectionUtils.isEmpty(roleMenus)) {
            return;
        }
        roleMenus.forEach(sysRoleMenuMapper::insert);
    }
}
