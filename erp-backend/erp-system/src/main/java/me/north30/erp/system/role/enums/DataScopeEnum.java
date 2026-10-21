package me.north30.erp.system.role.enums;

import java.util.Arrays;

/**
 * 角色数据范围枚举（sys_role.data_scope / sys_role_data_scope.scope_type，SYS-04）。
 * <p>合法取值 1~6、9；9-自定义的明细配置存 sys_role_data_scope 关联表。</p>
 */
public enum DataScopeEnum {

    /** 全部数据 */
    ALL(1, "全部"),
    /** 本组织及下级组织 */
    ORG_AND_CHILDREN(2, "本组织及下级"),
    /** 本组织 */
    ORG_ONLY(3, "本组织"),
    /** 本部门及下级部门 */
    DEPT_AND_CHILDREN(4, "本部门及下级"),
    /** 本部门 */
    DEPT_ONLY(5, "本部门"),
    /** 仅本人 */
    SELF_ONLY(6, "仅本人"),
    /** 自定义（明细见 sys_role_data_scope） */
    CUSTOM(9, "自定义");

    /** 数据范围编码（落库值） */
    private final int code;

    /** 提示文案 */
    private final String desc;

    DataScopeEnum(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /**
     * 判断编码是否为合法的数据范围取值。
     *
     * @param code 待校验编码
     * @return true 表示合法
     */
    public static boolean isValid(Integer code) {
        return code != null && Arrays.stream(values()).anyMatch(scope -> scope.code == code);
    }
}
