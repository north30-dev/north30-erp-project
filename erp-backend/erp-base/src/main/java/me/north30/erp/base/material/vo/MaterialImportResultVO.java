package me.north30.erp.base.material.vo;

import java.util.List;

/**
 * 物料导入结果 VO（接口文档 6.2.11/5.10 出参简化口径：CSV 导入，批次号追溯，D2 导入任务表未落地故无 taskId）。
 *
 * @param importBatch 导入批次号（IMP + 时间戳 + 随机段）
 * @param totalRows   总行数（不含表头）
 * @param successRows 成功行数
 * @param failRows    失败行数
 * @param errors      错误行明细（errorCode 取 11001-11999 区间或 10001）
 */
public record MaterialImportResultVO(
    String importBatch,
    Integer totalRows,
    Integer successRows,
    Integer failRows,
    List<RowError> errors
) {

    /**
     * 单行错误明细。
     *
     * @param rowNo        行号（数据行从 1 开始，不含表头）
     * @param errorCode    错误业务码
     * @param errorMessage 错误提示
     */
    public record RowError(
        Integer rowNo,
        Integer errorCode,
        String errorMessage
    ) {
    }
}
