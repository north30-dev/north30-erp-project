package me.north30.erp.system.core.vo;

/**
 * 通用写操作响应 VO（接口文档 5.5/5.6 写接口，按需携带字段）。
 *
 * @param id 记录 ID（新增返回，其余可为 null）
 * @param updateTime 更新时间（修改返回，其余可为 null）
 * @param isDeleted 逻辑删除标记（删除返回，其余可为 null）
 */
public record MutationVO(
    Long id,
    String updateTime,
    Integer isDeleted
) {
}
