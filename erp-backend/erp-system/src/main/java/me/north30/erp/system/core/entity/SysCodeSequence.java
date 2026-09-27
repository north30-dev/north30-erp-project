package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 单据编号序列表（sys_code_sequence）：前缀 + YYYYMM + 流水，唯一键 biz_type + period。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_code_sequence")
public class SysCodeSequence extends BaseEntity {

    /** 单据类型 SALES_ORDER/PURCHASE_ORDER/MO/FZ/STOCK_TRANSACTION 等 */
    private String bizType;

    /** 编号前缀（SO/PO/GR/MO/FZ/T 等） */
    private String prefix;

    /** 期间 YYYYMM（按月重置） */
    private String period;

    /** 当前已用流水号（原子递增） */
    private Integer currentNo;

    /** 流水位数（期初建账扩展为 5） */
    private Integer seqLength;
}
