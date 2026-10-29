package me.north30.erp.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 字典类型表（sys_dict_type）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_type")
public class SysDictType extends BaseEntity {

    /** 字典类型编码（如 settlement_method，全局唯一） */
    private String dictType;

    /** 字典类型名称 */
    private String dictName;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /**
     * 变更状态：仅值域守卫（10001），字典类型无内置保护。
     * <p>仅校验/修改自身状态，不查库。</p>
     *
     * @param targetStatus 目标状态 0-停用 1-启用
     */
    public void changeStatus(Integer targetStatus) {
        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态取值非法，仅支持 0-停用 1-启用");
        }
        this.status = targetStatus;
    }

    /**
     * 应用创建值默认值。
     * <p>
     * 仅在创建时应用，默认值。
     * status 状态默认值 1-启用
     * </p>
     */
    public void applyCreateDefault() {
        if (status == null) {
            status = 1;
        }
    }
}
