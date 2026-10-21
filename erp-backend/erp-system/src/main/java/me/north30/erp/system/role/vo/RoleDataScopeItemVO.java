package me.north30.erp.system.role.vo;

import java.util.List;

/**
 * 角色数据范围明细项响应 VO（接口文档 5.2.6/5.2.7 scopes[] 元素）。
 *
 * @param bizObject       业务对象
 * @param filterDimension 过滤维度
 * @param scopeType       数据范围档位
 * @param deptIds         部门 ID 集合
 * @param userIds         用户 ID 集合
 * @param fieldMask       字段掩码
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
