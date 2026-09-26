package me.north30.erp.common.audit;

/**
 * 审计操作类型枚举：固定动词词汇表，保证审计日志可按操作维度检索。
 */
public enum OperateTypeEnum {

    /** 创建 */
    CREATE("创建"),
    /** 修改 */
    UPDATE("修改"),
    /** 提交 */
    SUBMIT("提交"),
    /** 审核 */
    APPROVE("审核"),
    /** 驳回 */
    REJECT("驳回"),
    /** 取消 */
    CANCEL("取消"),
    /** 删除 */
    DELETE("删除");

    private final String desc;

    OperateTypeEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
