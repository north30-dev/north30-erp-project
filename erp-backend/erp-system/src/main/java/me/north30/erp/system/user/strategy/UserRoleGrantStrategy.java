package me.north30.erp.system.user.strategy;

import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.service.SysRoleDomainService;
import me.north30.erp.system.security.SecurityUtils;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.service.SysUserDomainService;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户角色授予守卫策略：角色分配链路的提权与自我降权守卫。
 * <p>跨域访问一律走目标域 Service（{@link SysRoleDomainService}/{@link SysUserDomainService}），
 * 禁止直接注入其他域 Mapper；用户-角色关联查询复用本域 {@link UserRoleStrategy} 收口。</p>
 */
@Component
@RequiredArgsConstructor
public class UserRoleGrantStrategy {

    private final SysRoleDomainService sysRoleDomainService;
    private final SysUserDomainService sysUserDomainService;
    private final UserRoleStrategy userRoleStrategy;
    private final UserAccessService userAccessService;

    /**
     * 提权守卫（18040）：目标角色含 ADMIN 时要求操作者为超管（08 文档 2.1），
     * 防止任何可操作角色分配者自我提权。create 与 assignRoles 共用。
     *
     * @param roleIds 本次待分配的角色 ID 集合
     */
    public void requireAssignAdminAllowed(List<Long> roleIds) {
        boolean containsAdmin = !roleIds.isEmpty()
            && sysRoleDomainService.listByIds(roleIds).stream()
                .anyMatch(role -> SysRole.ROLE_CODE_ADMIN.equals(role.getRoleCode()));
        if (!containsAdmin) {
            return;
        }
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        if (!userAccessService.isSuperAdmin(currentUserId)) {
            throw new BusinessException(SystemErrorCode.ONLY_SUPER_ADMIN_ASSIGN_ADMIN);
        }
    }

    /**
     * 自我降权守卫（18041）：操作者移除自己当前持有的 ADMIN 角色时拒绝。
     * <p>内置超管（is_admin=1）代码层仍具全量权限，不受此限；持 ADMIN 角色的普通操作者一旦摘除将失去全部入口、无法自愈。</p>
     *
     * @param targetUserId 目标用户 ID
     * @param roleIds      本次待分配的角色 ID 集合
     */
    public void requireNotRemoveOwnAdminRole(Long targetUserId, List<Long> roleIds) {
        Long currentUserId = SecurityUtils.requireCurrentUser().userId();
        if (!targetUserId.equals(currentUserId)) {
            return;
        }
        SysUser operator = sysUserDomainService.getById(currentUserId);
        if (operator != null && operator.isAdmin()) {
            return;
        }
        List<Long> heldRoleIds = userRoleStrategy.listRoleIdsByUserId(currentUserId);
        if (heldRoleIds.isEmpty()) {
            return;
        }
        boolean removesAdmin = sysRoleDomainService.listByIds(heldRoleIds).stream()
            .filter(role -> SysRole.ROLE_CODE_ADMIN.equals(role.getRoleCode()))
            .map(SysRole::getId)
            .anyMatch(adminRoleId -> !roleIds.contains(adminRoleId));
        if (removesAdmin) {
            throw new BusinessException(SystemErrorCode.SELF_OPERATION_FORBIDDEN,
                "不允许移除自己持有的超级管理员角色");
        }
    }
}
