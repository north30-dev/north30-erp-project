package me.north30.erp.base.materialcategory.vo;

/**
 * 物料分类状态变更响应 VO（POST /api/base/material-categories/{id}/status）。
 *
 * @param status 变更后的状态 0-停用 1-启用
 */
public record CategoryStatusVO(

    /** 变更后的状态 0-停用 1-启用 */
    Integer status
) {
}
