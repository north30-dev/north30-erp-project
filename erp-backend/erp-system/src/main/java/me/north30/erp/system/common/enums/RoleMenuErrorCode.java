package me.north30.erp.system.common.enums;

import lombok.Getter;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 角色与菜单管理域错误码（接口文档 2.10：18014-18023）。
 * <p>认证/用户段（18001-18011）见 common 的 {@code SystemErrorCode}，本枚举避让已用值；
 * 10601 属通用并发冲突段（10601-10699），为避免改动 common 暂挂本枚举。</p>
 */
@Getter
public enum RoleMenuErrorCode implements ErrorCode {

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

    /** 菜单超过三级（按钮下不可再建子节点） */
    MENU_LEVEL_EXCEEDED(18023, "菜单仅支持三级（目录/菜单/按钮）");

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    RoleMenuErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
