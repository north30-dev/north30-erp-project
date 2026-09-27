package me.north30.erp.system.core.vo;

import java.util.List;

/**
 * 角色数据范围明细项响应 VO（接口文档 5.2.6/5.2.7 scopes[] 元素）。
 */
public record RoleDataScopeItemVO(
    String bizObject,
    String filterDimension,
    Integer scopeType,
    List<Long> deptIds,
    List<Long> userIds,
    Integer fieldMask
) {
}
