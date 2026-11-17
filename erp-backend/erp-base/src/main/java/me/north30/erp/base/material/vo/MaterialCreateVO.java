package me.north30.erp.base.material.vo;

/**
 * 新增物料响应 VO（接口文档 6.2.4 出参）。
 *
 * @param id           新物料 ID
 * @param materialCode 服务端生成的物料编码（分类编码前 4 位 + 6 位流水）
 * @param status       初始状态 0-新建
 */
public record MaterialCreateVO(
    Long id,
    String materialCode,
    Integer status
) {
}
