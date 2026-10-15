package me.north30.erp.system.attachment.common;

/**
 * 附件域消费的 sys_config 参数 key 常量与兜底默认值（key 随消费域走，config 域不定义业务 key）。
 * <p>种子数据见 schema.sql（19/20 号，is_system=1）；读取统一走
 * {@code SysConfigDomainService.longValue(key, fallback)}，参数缺失/停用/非法时静默回落 fallback。</p>
 */
public final class AttachmentConfigKeys {

    /** 附件单文件大小上限（MB）对应的 sys_config key */
    public static final String MAX_FILE_SIZE_MB = "attachment.max.file.size.mb";

    /** 单据附件数量上限（个）对应的 sys_config key */
    public static final String MAX_COUNT_PER_BIZ = "attachment.max.count.per.biz";

    /** 单文件大小上限兜底值 20MB（接口文档 5.9.1） */
    public static final long DEFAULT_MAX_FILE_SIZE_MB = 20L;

    /** 单据附件数量上限兜底值 20 个（接口文档 5.9.1） */
    public static final long DEFAULT_MAX_COUNT_PER_BIZ = 20L;

    private AttachmentConfigKeys() {
    }
}
