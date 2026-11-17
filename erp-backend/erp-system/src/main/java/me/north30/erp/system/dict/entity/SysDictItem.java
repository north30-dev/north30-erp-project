package me.north30.erp.system.dict.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 字典项表（sys_dict_item）：同一 dict_type + item_value 可按 lang 存多行（E-03 预留，本期仅 zh-CN）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dict_item")
public class SysDictItem extends BaseEntity {

    /** 字典类型编码（sys_dict_type.dict_type） */
    private String dictType;

    /** 字典项标签（按 lang 存储对应语言文案） */
    private String itemLabel;

    /** 字典项值（编码值，不随语言变化） */
    private String itemValue;

    /** 语言（E-03 国际化预留：zh-CN/en-US） */
    private String lang;

    /** 显示顺序 */
    private Integer itemSort;

    /** 前端标签样式 */
    private String cssClass;

    /** 是否默认选中 0-否 1-是 */
    private Integer isDefault;

    /** 扩展属性 JSON */
    private String extJson;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /**
     * 变更状态：仅值域守卫（10001），字典项无内置保护。
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
     * itemSort 显示顺序默认值 0
     * isDefault 是否默认选中默认值 0-否 
     * status 状态默认值 1-启用
     * </p>
     */
    public void applyCreateDefault(){
        if (itemSort == null) {
            itemSort = 0;
        }
        if (isDefault == null) {
            isDefault = 0;
        }
        if (status == null) {
            status = 1;
        }
    }
}
