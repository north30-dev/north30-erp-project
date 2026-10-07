package me.north30.erp.system.log.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审计日志 VO（接口文档 5.8.1 分页 / 5.8.2 详情）。
 * <p>MyBatis 自动构造器映射承接 Mapper XML resultType 驼峰映射；
 * beforeJson/afterJson 仅详情接口返回，diffFields 为详情接口派生字段（withDiffFields 派生新实例）。</p>
 *
 * @param id            主键
 * @param module        业务模块 PURCHASE/SALES/INVENTORY/MANUFACTURING/FINANCE/SYSTEM
 * @param bizType       业务类型 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/STOCK_TRANSACTION 等
 * @param bizCode       单据号
 * @param operateType   操作类型 CREATE/UPDATE/APPROVE/REJECT/DELETE/POST/UNAPPROVE/CLOSE/REOPEN
 * @param operateDesc   操作描述
 * @param operateBy     操作人（用户名）
 * @param operateTime   操作时间
 * @param operateIp     操作 IP
 * @param requestUri    请求地址
 * @param requestMethod 请求方法
 * @param resultStatus  结果 0-失败 1-成功
 * @param errorCode     失败时的错误码
 * @param errorMessage  失败原因
 * @param costTime      方法耗时（毫秒）
 * @param traceId       链路追踪 ID
 * @param beforeJson    变更前 JSON（仅详情接口返回）
 * @param afterJson     变更后 JSON（仅详情接口返回）
 * @param diffFields    变更字段清单（详情接口派生：对比 before_json/after_json 键值差异）
 */
public record AuditLogVO(
    Long id,
    String module,
    String bizType,
    String bizCode,
    String operateType,
    String operateDesc,
    String operateBy,
    LocalDateTime operateTime,
    String operateIp,
    String requestUri,
    String requestMethod,
    Integer resultStatus,
    Integer errorCode,
    String errorMessage,
    Long costTime,
    String traceId,
    String beforeJson,
    String afterJson,
    List<String> diffFields
) {

    /**
     * 派生携带变更字段清单的新实例（record 不可变）。
     */
    public AuditLogVO withDiffFields(List<String> diffFields) {
        return new AuditLogVO(id, module, bizType, bizCode, operateType, operateDesc, operateBy,
            operateTime, operateIp, requestUri, requestMethod, resultStatus, errorCode, errorMessage,
            costTime, traceId, beforeJson, afterJson, diffFields);
    }
}
