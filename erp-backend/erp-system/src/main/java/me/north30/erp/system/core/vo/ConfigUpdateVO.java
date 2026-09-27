package me.north30.erp.system.core.vo;

/**
 * 修改系统参数响应 VO（接口文档 5.6.3）。
 *
 * @param effectNote 生效提示（固定"参数将在 1 分钟内生效"）
 * @param updateTime 更新时间（yyyy-MM-dd HH:mm:ss）
 */
public record ConfigUpdateVO(
    String effectNote,
    String updateTime
) {
}
