package me.north30.erp.system.config.vo;

/**
 * 系统参数状态变更响应 VO。
 *
 * @param status 变更后的状态 0-停用 1-启用
 */
public record ConfigStatusVO(

    /** 变更后的状态 0-停用 1-启用 */
    Integer status
) {
}
