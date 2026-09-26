package me.north30.erp.common.audit;

/**
 * 审计业务模块枚举：取值对齐错误码分区（物料/BOM、采购、销售、库存、生产、财务）。
 */
public enum AuditModuleEnum {

    /** 系统管理 */
    SYSTEM("系统管理"),
    /** 物料/BOM */
    MATERIAL("物料/BOM"),
    /** 采购 */
    PURCHASE("采购"),
    /** 销售 */
    SALES("销售"),
    /** 库存 */
    INVENTORY("库存"),
    /** 生产 */
    MANUFACTURING("生产"),
    /** 财务 */
    FINANCE("财务");

    private final String desc;

    AuditModuleEnum(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }
}
