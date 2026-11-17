package me.north30.erp.base.material.common;

/**
 * 物料域消费的数据字典 key 常量（key 随消费域走，dict 域不定义业务 key，
 * 对齐 system 附件域 {@code AttachmentDictKeys} 先例）。
 * <p>词表种子数据见 database/init/01_seed.sql（sys_dict_type id=1/2/3/13/16）；
 * 服务端校验统一走 common {@code DictValidator.requireEnabledItem}（不在词表抛 11016），
 * 展示标签统一走 common {@code DictOptionsResolver.loadItemLabels}（导出/渲染消除文案硬编码）。</p>
 */
public final class MaterialDictKeys {

    /** 计量单位（服务端校验：11016 计量单位非法） */
    public static final String UNIT = "unit";

    /** 物料类型 1-原材料 2-半成品 3-成品 4-辅料（导出 label 渲染） */
    public static final String MATERIAL_TYPE = "material_type";

    /** 来源类型 1-自制 2-外购 3-外协（导出 label 渲染） */
    public static final String SOURCE_TYPE = "source_type";

    /** 物料状态 0-新建 1-审核 2-启用 3-停用（导出 label 渲染；流转语义以实体常量为准） */
    public static final String MATERIAL_STATUS = "material_status";

    /** ABC 分类 A/B/C（数值域守卫见 DTO @Pattern，导出原值展示） */
    public static final String ABC_CLASS = "abc_class";

    private MaterialDictKeys() {
    }
}
