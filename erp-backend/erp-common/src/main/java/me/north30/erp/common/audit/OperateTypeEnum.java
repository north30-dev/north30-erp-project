package me.north30.erp.common.audit;

/**
 * 审计操作类型枚举：固定动词词汇表，保证审计日志可按操作维度检索。
 */
public enum OperateTypeEnum {

    /** 创建 */
    CREATE("创建"),
    /** 修改 */
    UPDATE("修改"),
    /** 审核 */
    APPROVE("审核"),
    /** 驳回 */
    REJECT("驳回"),
    /** 删除 */
    DELETE("删除"),
    /** 过账 */
    POST("过账"),
    /** 反审核 */
    UNAPPROVE("反审核"),
    /** 关闭 */
    CLOSE("关闭"),
    /** 期间重开 */
    REOPEN("期间重开");

    private final String desc;

    OperateTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
