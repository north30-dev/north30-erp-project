package me.north30.erp.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemErrorCode;

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

    /**
     * 类型级启用守卫：status != 1 时抛 18043。
     * <p>仅校验自身状态，不查库；供选项查询（options）、词表校验（requireEnabledItem）
     * 等消费链路复用，保证类型禁用后整类字典一起下线。</p>
     */
    public void requireEnabled() {
        if (status == null || status != 1) {
            throw new BusinessException(SystemErrorCode.DICT_TYPE_DISABLED);
        }
    }
}
