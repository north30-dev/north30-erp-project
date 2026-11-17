package me.north30.erp.system.user.strategy;

import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.service.SysRoleDomainService;
import me.north30.erp.system.security.SecurityUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 用户角色授予守卫策略：角色分配链路的提权与自我降权守卫。
 * <p>跨域访问一律走目标域 Service（{@link SysRoleDomainService}），
 * 禁止直接注入其他域 Mapper。</p>
 */
@Component
@RequiredArgsConstructor
public class UserRoleGrantStrategy {

    private final SysRoleDomainService sysRoleDomainService;
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
     * 自我操作守卫（18041）：禁止修改自己的角色。
     * <p>任何自我角色变更（增/删/清空）都可被多步合法操作组合出自我提权（先加后摘可换持任意可授角色）
     * 或无法自愈的自我降权（摘 ADMIN/清空后失去全部入口），故一律拒绝；
     * 内置超管凭 is_admin=1 已具备代码层全量权限，无需变更；角色调整应由其他管理员操作。</p>
     *
     * @param targetUserId 目标用户 ID
     */
    public void requireNotSelfOperation(Long targetUserId) {
        if (SecurityUtils.requireCurrentUser().userId().equals(targetUserId)) {
            throw new BusinessException(SystemErrorCode.SELF_OPERATION_FORBIDDEN,
                "不允许修改自己的角色");
        }
    }
}
