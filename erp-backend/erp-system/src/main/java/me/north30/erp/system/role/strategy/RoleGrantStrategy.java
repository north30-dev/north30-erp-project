package me.north30.erp.system.role.strategy;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.role.converter.RoleConverter;
import me.north30.erp.system.role.dto.RoleDataScopeItemDTO;
import me.north30.erp.system.role.entity.SysRoleDataScope;
import me.north30.erp.system.role.entity.SysRoleMenu;
import me.north30.erp.system.role.enums.DataScopeEnum;
import me.north30.erp.system.role.mapper.SysRoleDataScopeMapper;
import me.north30.erp.system.role.mapper.SysRoleMenuMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 角色授权策略：菜单授权与数据范围配置的全删全插、删除角色时的关联清理。
 */
@Component
@RequiredArgsConstructor
public class RoleGrantStrategy {

    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final SysRoleDataScopeMapper sysRoleDataScopeMapper;
    private final RoleConverter roleConverter;

    /**
     * 全删全插重写角色-菜单授权（先逻辑删除旧关联，再逐条插入新授权）。
     */
    public void replaceRoleMenus(Long roleId, List<Long> menuIds) {
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        for (Long menuId : menuIds) {
            sysRoleMenuMapper.insert(roleConverter.toSysRoleMenu(roleId, menuId));
        }
    }

    /**
     * 全删全插重写角色数据范围配置（deptIds/userIds 序列化与 fieldMask 缺省由调用方装配）。
     */
    public void replaceDataScopes(Long roleId, List<SysRoleDataScope> scopes) {
        sysRoleDataScopeMapper.delete(
            new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, roleId));
        scopes.forEach(sysRoleDataScopeMapper::insert);
    }

    /**
     * 删除角色时级联逻辑删除菜单授权与数据范围配置，避免残留引用阻塞菜单删除/范围查询。
     */
    public void cleanupOnRoleDelete(Long roleId) {
        sysRoleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, roleId));
        sysRoleDataScopeMapper.delete(
            new LambdaQueryWrapper<SysRoleDataScope>().eq(SysRoleDataScope::getRoleId, roleId));
    }

    /**
     * 数据范围明细 DTO → 实体（deptIds/userIds 序列化与 fieldMask 缺省值在此装配）。
     * <p>装配点承载范围校验：scopeType 值域守卫下沉实体（{@link SysRoleDataScope#requireScopeTypeValid()}）；
     * 自定义档位（9）须至少勾选组织或人员（joinIds 序列化后的入参字符串判空）。</p>
     */
    public SysRoleDataScope buildDataScope(Long roleId, RoleDataScopeItemDTO item,
                                           String deptIds, String userIds) {
        SysRoleDataScope scope = roleConverter.toSysRoleDataScope(roleId, item);
        scope.requireScopeTypeValid();
        if (DataScopeEnum.CUSTOM.getCode() == scope.getScopeType()
            && !StringUtils.hasText(deptIds) && !StringUtils.hasText(userIds)) {
            throw new BusinessException(SystemErrorCode.ROLE_DATA_SCOPE_INVALID,
                "数据范围配置非法：自定义范围必须至少勾选组织或人员");
        }
        scope.setDeptIds(deptIds);
        scope.setUserIds(userIds);
        scope.setFieldMask(item.fieldMask() == null ? 0 : item.fieldMask());
        return scope;
    }
}
