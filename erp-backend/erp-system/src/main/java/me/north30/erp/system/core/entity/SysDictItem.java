package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
}
