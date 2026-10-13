package me.north30.erp.system.codesequence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemErrorCode;

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

    public void validateResetCurrentNo(Integer currentNo) {
        if (currentNo == null || currentNo < 0) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, 
                "当前重置流水号不能为空或小于 0");
        }
        if (currentNo < this.currentNo){
            throw new BusinessException(SystemErrorCode.SEQUENCE_RESET_CONFLICT,
                "流水号重置值不得小于当前已用流水号 " + this.currentNo);
        }
    }
}
