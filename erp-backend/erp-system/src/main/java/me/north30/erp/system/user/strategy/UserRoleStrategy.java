package me.north30.erp.system.user.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.user.converter.UserConverter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 用户-角色关联策略：sys_user_role/sys_role 跨域读写的统一收口。
 * <p>IO 类策略（@Component）；角色存在性校验、关联全删全插、引用计数与角色 ID 查询
 * 均收敛于此，避免各调用方散落重复实现。</p>
 */
@Component
@RequiredArgsConstructor
public class UserRoleStrategy {

    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final UserConverter userConverter;

    /**
     * 校验角色集合全部存在，否则抛 18014（一次 IN 计数，非循环查库）。
     */
    public void requireAllExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
        if (count == null || count != roleIds.size()) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND,
                "角色不存在，请刷新角色列表后重试");
        }
    }

    /**
     * 写入用户-角色关联（单用户角色数个位数，逐条插入即可，无需批量基建）。
     */
    public void insertForUser(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            sysUserRoleMapper.insert(userConverter.toUserRole(userId, roleId));
        }
    }

    /**
     * 全删全插（全量覆盖语义）：sys_user_role 带 @TableLogic，delete 为逻辑删除，
     * 唯一索引 uk_sys_user_role 仅约束 is_deleted=0 行，重插不冲突。
     */
    public void replaceAllForUser(Long userId, List<Long> roleIds) {
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        insertForUser(userId, roleIds);
    }

    /**
     * 统计用户已分配角色数（删除引用守卫用）。
     */
    public long countByUserId(Long userId) {
        Long count = sysUserRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        return count != null ? count : 0L;
    }

    /**
     * 查询用户已分配角色 ID 列表（去空去重）。
     */
    public List<Long> listRoleIdsByUserId(Long userId) {
        return sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId)).stream()
            .map(SysUserRole::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }
}
