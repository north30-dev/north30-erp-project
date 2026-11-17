package me.north30.erp.system.attachment.common;

/**
 * 附件域消费的数据字典 key 常量（key 随消费域走，dict 域不定义业务 key）。
 * <p>词表种子数据见 01_seed.sql（字典类型 id=27）；校验统一走
 * {@code SysDictItemService.requireEnabledItem(dictType, itemValue, lang)}，不在词表的取值返回 10404。</p>
 */
public final class AttachmentDictKeys {

    /** 附件关联业务类型对应的字典类型编码（sys_dict_type.dict_type） */
    public static final String DICT_TYPE = "attachment_biz_type";

    private AttachmentDictKeys() {
    }
}
