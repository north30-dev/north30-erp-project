package me.north30.erp.system.core.enums;

import lombok.Getter;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 系统管理域扩展错误码（组织/字典/参数/编号序列，接口文档 2.10）。
 * <p>18001-18011 已在 erp-common 的 {@code SystemErrorCode} 定义，本枚举避让已用值，
 * 仅补充本域新增码；10601 为通用区间乐观锁冲突码（erp-common 未定义，本域先登记）。</p>
 */
@Getter
public enum SystemManageErrorCode implements ErrorCode {

    /** 数据已被其他操作修改（乐观锁 version 冲突） */

    /** 组织不存在 */
    DEPT_NOT_FOUND(18024, "组织不存在"),

    /** 组织存在下级组织或绑定用户不可删除 */
    DEPT_HAS_CHILDREN_OR_USERS(18025, "组织存在下级组织或绑定用户，不可删除"),

    /** 组织编码已存在 */
    DEPT_CODE_EXISTS(18026, "组织编码已存在"),

    /** 组织层级超限（超过 5 级） */
    DEPT_LEVEL_EXCEED(18027, "组织层级超过上限"),

    /** 字典类型不存在 */
    DICT_TYPE_NOT_FOUND(18028, "字典类型不存在"),

    /** 字典类型已存在 */
    DICT_TYPE_EXISTS(18029, "字典类型已存在"),

    /** 字典项被引用不可删除 */
    DICT_ITEM_REFERENCED(18030, "字典项被引用不可删除"),

    /** 字典项重复（同 dict_type + item_value + lang 冲突） */
    DICT_ITEM_EXISTS(18031, "字典项已存在"),

    /** 系统参数不存在 */
    CONFIG_NOT_FOUND(18032, "系统参数不存在"),

    /** 内置参数不可删除 */
    BUILTIN_CONFIG_UNDELETABLE(18033, "内置系统参数不可删除"),

    /** 系统参数值类型错误 */
    CONFIG_VALUE_TYPE_MISMATCH(18034, "参数值类型错误，期望类型 {valueTypeName}"),

    /** 编号序列不存在 */
    SEQUENCE_NOT_FOUND(18035, "编号序列不存在"),

    /** 流水号重置值不得小于当前已用流水号 */
    SEQUENCE_RESET_CONFLICT(18036, "流水号重置值不得小于当前已用流水号 {currentNo}"),

    /** 附件：文件大小超出上限 */
    ATTACHMENT_TYPE_NOT_SUPPORTED(10401, "文件类型不支持"),

    /** 附件：文件类型不支持 */
    ATTACHMENT_SIZE_EXCEEDED(10402, "文件大小超出上限"),

    /** 附件：业务对象附件数量超限 */
    ATTACHMENT_COUNT_EXCEEDED(10403, "附件数量超出上限"),

    /** 附件：附件不存在 */
    AUDIT_LOG_NOT_FOUND(18037, "审计日志不存在"),

    /** 日志：审计日志不存在 */
    ATTACHMENT_NOT_FOUND(18038, "附件不存在"),

    /** 数据权限：仅创建人可操作 */
    DATA_SCOPE_DENIED(10202, "无权操作该数据"),

    /** 导入：导入任务不存在 */
    IMPORT_TASK_NOT_FOUND(10503, "导入任务不存在");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    SystemManageErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
