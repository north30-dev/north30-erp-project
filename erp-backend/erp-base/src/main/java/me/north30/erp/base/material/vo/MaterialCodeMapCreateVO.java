package me.north30.erp.base.material.vo;

/**
 * 新增/修改物料旧码映射响应 VO（接口文档 6.2.15 出参）。
 *
 * @param id              映射记录 ID
 * @param newMaterialCode 服务端按 newMaterialId 回填的新物料编码
 */
public record MaterialCodeMapCreateVO(
    Long id,
    String newMaterialCode
) {
}
