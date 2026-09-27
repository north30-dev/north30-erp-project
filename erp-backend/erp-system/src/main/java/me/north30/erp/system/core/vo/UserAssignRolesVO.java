package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 分配用户角色响应 VO（接口文档 5.1.8 出参：保存后的角色 ID 集合与权限刷新提示）。
 */
public record UserAssignRolesVO(
    List<Long> roleIds,
    String permissionRefreshTip
) {
}
