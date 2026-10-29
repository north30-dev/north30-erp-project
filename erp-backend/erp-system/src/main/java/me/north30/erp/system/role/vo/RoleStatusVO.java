package me.north30.erp.system.role.vo;

/**
 * 角色状态变更响应 VO。
 *
 * @param status 变更后的状态 0-停用 1-启用
 */
public record RoleStatusVO(

    /** 变更后的状态 0-停用 1-启用 */
    Integer status
) {
}
