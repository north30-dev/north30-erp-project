package me.north30.erp.system.common.enums;

import lombok.Getter;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 系统管理域错误码（18001-18999，接口文档 2.10，模块内唯一来源）。
 * <p>号段独占：18001-18013 认证/用户、18014-18023 角色与菜单、18024-18038 组织/字典/参数/编号序列/日志/附件。
 * 通用段码（10202/10401-10403/10503）见 {@code me.north30.erp.common.exception.CommonErrorCode}。</p>
 */
@Getter
public enum SystemErrorCode implements ErrorCode {

    // ---------- 认证（18001-18004） ----------

    /** 用户名或密码错误 */
    USERNAME_PASSWORD_ERROR(18001, "用户名或密码错误"),

    /** 账号已锁定（连续 5 次密码错误，锁定 30 分钟） */
    ACCOUNT_LOCKED(18002, "账号已锁定，请 {unlockMinutes} 分钟后重试"),

    /** 账号已停用 */
    ACCOUNT_DISABLED(18003, "账号已停用，请联系管理员"),

    /** 验证码错误或已过期 */
    CAPTCHA_ERROR(18004, "验证码错误或已过期"),

    // ---------- 用户（18005-18013） ----------

    /** 用户不存在 */
    USER_NOT_FOUND(18005, "用户不存在"),

    /** 用户名已存在（username 唯一冲突） */
    USERNAME_EXISTS(18006, "用户名 {username} 已存在"),

    /** 用户编号已存在（user_code 唯一冲突） */
    USER_CODE_EXISTS(18007, "用户编号 {userCode} 已存在"),

    /** 原密码错误 */
    OLD_PASSWORD_ERROR(18008, "原密码错误"),

    /** 新密码不符合复杂度要求 */
    PASSWORD_COMPLEXITY_ERROR(18009, "新密码须不少于 8 位且包含大写字母、小写字母、数字、特殊字符中的至少 3 类"),

    /** 新密码与历史密码重复 */
    PASSWORD_HISTORY_CONFLICT(18010, "新密码不得与最近 3 次使用的密码相同"),

    /** 口令已过期需修改 */
    PASSWORD_EXPIRED(18011, "口令已超过 90 天有效期，请先修改密码"),

    /** 内置管理员不可操作（删除/停用 is_admin=1 用户） */
    ADMIN_PROTECTED(18012, "内置超级管理员不可删除或停用"),

    /** 用户已被引用不可删除（存在角色分配等关联） */
    USER_REFERENCED(18013, "用户 {username} 已被引用，不可删除（请停用）"),

    // ---------- 角色与菜单（18014-18023） ----------

    /** 角色不存在 */
    ROLE_NOT_FOUND(18014, "角色不存在"),

    /** 角色编码已存在（role_code 唯一冲突） */
    ROLE_CODE_EXISTS(18015, "角色编码已存在"),

    /** 角色被用户引用不可删除 */
    ROLE_REFERENCED_BY_USER(18016, "角色已分配用户，不可删除"),

    /** 内置角色不可删除 */
    ROLE_BUILTIN(18017, "内置角色不可删除"),

    /** 角色数据范围配置非法 */
    ROLE_DATA_SCOPE_INVALID(18018, "数据范围配置非法"),

    /** 菜单不存在 */
    MENU_NOT_FOUND(18019, "菜单不存在"),

    /** 菜单存在子节点不可删除 */
    MENU_HAS_CHILDREN(18020, "菜单存在子节点，不可删除"),

    /** 菜单已被角色引用不可删除 */
    MENU_REFERENCED_BY_ROLE(18021, "菜单已被角色引用，不可删除"),

    /** 权限标识重复（perms 唯一冲突） */
    MENU_PERMS_DUPLICATED(18022, "权限标识已存在"),

    /** 菜单层级超过上限（按钮下不可建子；深度最多 4 级） */
    MENU_LEVEL_EXCEEDED(18023, "菜单层级超过上限（最多 4 级）"),

    // ---------- 组织（18024-18027） ----------

    /** 组织不存在 */
    DEPT_NOT_FOUND(18024, "组织不存在"),

    /** 组织存在下级组织或绑定用户不可删除 */
    DEPT_HAS_CHILDREN_OR_USERS(18025, "组织存在下级组织或绑定用户，不可删除"),

    /** 组织编码已存在 */
    DEPT_CODE_EXISTS(18026, "组织编码已存在"),

    /** 组织层级超限（超过 5 级） */
    DEPT_LEVEL_EXCEED(18027, "组织层级超过上限"),

    // ---------- 字典（18028-18031） ----------

    /** 字典类型不存在 */
    DICT_TYPE_NOT_FOUND(18028, "字典类型不存在"),

    /** 字典类型已存在 */
    DICT_TYPE_EXISTS(18029, "字典类型已存在"),

    /** 字典项被引用不可删除 */
    DICT_ITEM_REFERENCED(18030, "字典项被引用不可删除"),

    /** 字典项重复（同 dict_type + item_value + lang 冲突） */
    DICT_ITEM_EXISTS(18031, "字典项已存在"),

    // ---------- 系统参数（18032-18034） ----------

    /** 系统参数不存在 */
    CONFIG_NOT_FOUND(18032, "系统参数不存在"),

    /** 内置参数不可删除 */
    BUILTIN_CONFIG_UNDELETABLE(18033, "内置系统参数不可删除"),

    /** 系统参数值类型错误 */
    CONFIG_VALUE_TYPE_MISMATCH(18034, "参数值类型错误，期望类型 {valueTypeName}"),

    // ---------- 编号序列（18035-18036） ----------

    /** 编号序列不存在 */
    SEQUENCE_NOT_FOUND(18035, "编号序列不存在"),

    /** 流水号重置值不得小于当前已用流水号 */
    SEQUENCE_RESET_CONFLICT(18036, "流水号重置值不得小于当前已用流水号 {currentNo}"),

    // ---------- 日志与附件（18037-18038） ----------

    /** 审计日志不存在 */
    AUDIT_LOG_NOT_FOUND(18037, "审计日志不存在"),

    /** 附件不存在 */
    ATTACHMENT_NOT_FOUND(18038, "附件不存在");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    SystemErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
