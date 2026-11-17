package me.north30.erp.base.material.vo;

/**
 * 物料状态流转响应 VO（接口文档 6.2.8/6.2.9/6.2.10 出参）。
 *
 * @param status           变更后状态 0-新建 1-审核 2-启用 3-停用
 * @param affectedBomCount 引用该物料的非历史 BOM 数（停用接口返回，仅提示不拦截；BOM 域落地前恒为 0，其余流转为 null）
 */
public record MaterialStatusVO(
    Integer status,
    Integer affectedBomCount
) {
}
