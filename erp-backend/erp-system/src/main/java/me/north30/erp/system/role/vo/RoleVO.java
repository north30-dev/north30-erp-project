package me.north30.erp.system.role.vo;

/**
 * 角色分页记录响应 VO（接口文档 5.2.1，userCount 为 sys_user_role 派生计数）。
 *
 * @param id        角色 ID
 * @param roleCode  角色编码
 * @param roleName  角色名称
 * @param roleSort  排序号
 * @param dataScope 数据范围
 * @param isBuiltin 是否内置角色
 * @param status    状态
 * @param userCount 关联用户数
 */
public record RoleVO(
    Long id,
    String roleCode,
    String roleName,
    Integer roleSort,
    Integer dataScope,
    Integer isBuiltin,
    Integer status,
    Integer userCount
) {
}
