package me.north30.erp.common.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解：标注在需要记录操作审计的写操作方法上
 * （订单/工单/出入库单的创建、修改、审核、删除等），由 {@link AuditLogAspect} 统一记录。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuditLog {

    /** 业务模块，对齐错误码分区 */
    AuditModuleEnum module();

    /** 操作类型 */
    OperateTypeEnum operateType();

    /** 单据编号/业务标识，如：PO20260918001 */
    String bizCode() default "";
}
