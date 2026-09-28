package me.north30.erp.system.role.vo;

import java.util.List;

/**
 * 角色数据范围响应 VO（接口文档 5.2.6/5.2.7）。
 */
public record RoleDataScopeVO(
    Integer dataScope,
    List<RoleDataScopeItemVO> scopes
) {
}
