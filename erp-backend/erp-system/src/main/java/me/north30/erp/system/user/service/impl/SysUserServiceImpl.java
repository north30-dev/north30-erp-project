package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.user.service.SysUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 用户服务实现。
 */
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl implements SysUserService {

    private final SysUserMapper sysUserMapper;

    /**
     * 根据用户名查询用户。
     * 
     * @param username 用户名。
     * @return 用户实体。
     */
    @Override
    @Transactional(readOnly = true)
    public SysUser getByUsername(String username) {
        return sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
    }

    /**
     * 根据 ID 查询用户。
     * 
     * @param id 用户 ID。
     * @return 用户实体。
     */
    @Override
    @Transactional(readOnly = true)
    public SysUser getById(Long id) {
        return sysUserMapper.selectById(id);
    }

    /**
     * 新增用户。
     * 
     * @param user 用户实体。
     * @return 是否新增成功。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createUser(SysUser user) {
        return sysUserMapper.insert(user) > 0;
    }

    /**
     * 更新用户。
     * 
     * @param user 用户实体。
     * @return 是否更新成功。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateUser(SysUser user) {
        return sysUserMapper.updateById(user) > 0;
    }

    /**
     * 更新用户最后登录信息。
     * 
     * @param userId 用户 ID。
     * @param loginIp 登录 IP。
     * @param loginTime 登录时间。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastLogin(Long userId, String loginIp, LocalDateTime loginTime) {
        SysUser user = new SysUser();
        user.setId(userId);
        user.setLastLoginTime(loginTime);
        user.setLastLoginIp(loginIp);
        user.setLoginFailCount(0);
        sysUserMapper.updateById(user);
    }
}
