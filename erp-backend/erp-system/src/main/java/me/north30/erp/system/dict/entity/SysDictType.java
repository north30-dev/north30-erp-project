package me.north30.erp.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
