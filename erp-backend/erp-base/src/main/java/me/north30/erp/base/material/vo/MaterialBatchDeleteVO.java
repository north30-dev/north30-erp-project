package me.north30.erp.base.material.vo;

import java.util.List;

/**
 * 物料批量删除响应 VO（接口文档 6.2.7 出参，部分失败语义见 13.1）。
 *
 * @param successCount 成功删除条数
 * @param failCount    失败条数
 * @param failures     失败明细（逐行采集失败原因）
 */
public record MaterialBatchDeleteVO(
    Integer successCount,
    Integer failCount,
    List<Failure> failures
) {

    /**
     * 单行失败明细。
     *
     * @param id           物料 ID
     * @param materialCode 物料编码（物料不存在时为 null）
     * @param errorCode    失败原因业务码（如 11002/11009/11011）
     * @param errorMessage 失败原因提示
     */
    public record Failure(
        Long id,
        String materialCode,
        Integer errorCode,
        String errorMessage
    ) {
    }
}
