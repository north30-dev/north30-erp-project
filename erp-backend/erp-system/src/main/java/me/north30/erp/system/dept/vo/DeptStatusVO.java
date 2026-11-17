package me.north30.erp.system.dept.vo;

/**
 * 组织/部门状态变更响应 VO。
 *
 * @param status 变更后的状态 0-停用 1-启用
 */
public record DeptStatusVO(

    /** 变更后的状态 0-停用 1-启用 */
    Integer status
) {
}
