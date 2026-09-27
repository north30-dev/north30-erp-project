package me.north30.erp.system.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.core.entity.SysMenu;
import me.north30.erp.system.core.mapper.SysMenuMapper;
import me.north30.erp.system.core.service.ISysMenuService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

/**
 * 菜单与权限点服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements ISysMenuService {

    private final SysMenuMapper sysMenuMapper;

    @Override
    @Transactional(readOnly = true)
    public SysMenu getByMenuName(String menuName) {
        return sysMenuMapper.selectOne(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getMenuName, menuName));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listEnabled() {
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<SysMenu> listEnabledByIds(Collection<Long> ids) {
        if (CollectionUtils.isEmpty(ids)) {
            return List.of();
        }
        return sysMenuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
            .in(SysMenu::getId, ids)
            .eq(SysMenu::getStatus, 1));
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> listAllPerms() {
        return listEnabled().stream()
            .map(SysMenu::getPerms)
            .filter(perms -> perms != null && !perms.isBlank())
            .distinct()
            .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createMenu(SysMenu menu) {
        return sysMenuMapper.insert(menu) > 0;
    }
}
