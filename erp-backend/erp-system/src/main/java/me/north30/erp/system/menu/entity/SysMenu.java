package me.north30.erp.system.menu.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemErrorCode;

import java.util.regex.Pattern;

/**
 * 菜单与按钮权限点表（sys_menu）。
 * <p>层级规则（按钮只能挂菜单下，其余子类型不得小于父类型）：
 * 顶级 → 目录/菜单；目录 → 目录/菜单；菜单 → 菜单/按钮；按钮 → 不允许。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    /** 菜单类型：目录 */
    public static final int TYPE_DIR = 1;
    /** 菜单类型：菜单 */
    public static final int TYPE_MENU = 2;
    /** 菜单类型：按钮 */
    public static final int TYPE_BUTTON = 3;

    /** 菜单树最大深度（模块 → 子模块 → 页面 → 按钮） */
    public static final int MAX_DEPTH = 4;

    /** 路由地址：以 / 开头，小写字母/数字/中划线分段，无连续或结尾斜杠 */
    private static final Pattern PATH_PATTERN = Pattern.compile("^/[a-z0-9-]+(?:/[a-z0-9-]+)*$");

    /** 组件路径：相对 views 目录（不以 / 开头、不以 .vue 结尾），至少两级；段允许大驼峰（与前端页面组件命名规范一致），如 system/loginLog/index */
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[a-zA-Z0-9-]+(?:/[a-zA-Z0-9-]+)+$");

    /** 权限标识：固定三段 模块:对象:动作，小写，如 system:menu:create */
    private static final Pattern PERMS_PATTERN =
        Pattern.compile("^[a-z][a-z0-9-]*:[a-z][a-z0-9-]*:[a-z][a-z0-9-]*$");

    /** 菜单/权限点名称 */
    private String menuName;

    /** 父级菜单 ID（0 为顶级） */
    private Long parentId;

    /** 类型 1-目录 2-菜单 3-按钮 */
    private Integer menuType;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 权限点标识（如 sales:order:approve） */
    private String perms;

    /** 图标 */
    private String icon;

    /** 显示顺序 */
    private Integer menuSort;

    /** 是否显示 0-隐藏 1-显示 */
    private Integer visible;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 是否内置菜单 0-否 1-是（种子菜单即内置，不可停用/删除，不随父级继承） */
    private Integer isSystem;

    /**
     * 变更状态：值域守卫（10001）+ 内置菜单不可停用守卫（18039）。
     * <p>内置菜单（isSystem=1）允许启用、拒绝停用；停用内置子树级联拦截由
     * DomainService 查库承载（本方法不查库）。</p>
     *
     * @param targetStatus 目标状态 0-停用 1-启用
     */
    public void changeStatus(Integer targetStatus) {
        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态值非法：" + targetStatus);
        }
        if (targetStatus == 0 && Integer.valueOf(1).equals(isSystem)) {
            throw new BusinessException(SystemErrorCode.SYSTEM_MENU_PROTECTED);
        }
        this.status = targetStatus;
    }

    /**
     * 创建时应用默认值。
     * <p>默认值：menuSort=0, visible=1, status=1, isSystem=0</p>
     * <p>仅在创建时调用，更新时不调用。</p>
     */
    public void applyCreateDefaults() {
        if (menuSort == null) {
            menuSort = 0;
        }
        if (visible == null) {
            visible = 1;
        }
        if (status == null) {
            status = 1;
        }
        if (isSystem == null) {
            isSystem = 0;
        }
    }

    /**
     * 校验父子类型配对（不查库，仅依据两个类型值判定）。
     * <p>规则：顶级仅允许目录/菜单；目录下仅允许目录/菜单；菜单下仅允许菜单/按钮；
     * 按钮下不可建子节点（18023）。父类型非法抛 10001。</p>
     *
     * @param parentType 父级菜单类型；顶级传 {@code null}
     * @param childType  待挂载的子节点类型
     */
    public static void requireValidChildType(Integer parentType, Integer childType) {
        if (childType == null || childType < TYPE_DIR || childType > TYPE_BUTTON) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "菜单类型非法：" + childType);
        }
        if (parentType == null) {
            if (childType == TYPE_BUTTON) {
                throw new BusinessException(CommonErrorCode.PARAM_ERROR, "顶级仅支持目录或菜单类型");
            }
            return;
        }
        if (parentType == TYPE_BUTTON) {
            throw new BusinessException(SystemErrorCode.MENU_LEVEL_EXCEEDED);
        }
        if (parentType == TYPE_DIR && childType == TYPE_BUTTON) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "目录下仅支持目录或菜单类型");
        }
        if (parentType == TYPE_MENU && childType != TYPE_MENU && childType != TYPE_BUTTON) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "菜单下仅支持菜单或按钮类型");
        }
    }

    /**
     * 校验菜单类型与按类型的必填、格式字段：
     * 目录/菜单必填 path（格式如 /system/menu），菜单还需 component（格式如 system/menu/index），
     * 按钮必填 perms（格式如 system:menu:create）。校验通过后将三个字段 trim 归一化。
     * <p>仅校验自身状态，非法抛 10001 参数错误。</p>
     */
    public void requireTypeFieldsValid() {
        if (menuType == null || menuType < TYPE_DIR || menuType > TYPE_BUTTON) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "菜单类型非法：" + menuType);
        }
        if (menuType == TYPE_DIR || menuType == TYPE_MENU) {
            this.path = requirePattern(PATH_PATTERN, path, "路由地址", "/system/menu");
        }
        if (menuType == TYPE_MENU) {
            this.component = requirePattern(COMPONENT_PATTERN, component, "前端组件路径", "system/menu/index");
        }
        if (menuType == TYPE_BUTTON) {
            this.perms = requirePattern(PERMS_PATTERN, perms, "权限标识", "system:menu:create");
        }
    }

     /**
     * 按钮不承载路由与组件信息，置空 path/component。
     */
    public void clearRouteFieldsIfButton() {
        if (Integer.valueOf(TYPE_BUTTON).equals(menuType)) {
            this.path = null;
            this.component = null;
        }
    }

    /**
     * 校验字段非空且匹配 pattern，通过则返回 trim 后的值，否则抛 10001。
     * 
     * @param pattern 正则表达式模式
     * @param value   待校验值
     * @param fieldName 字段名
     * @param example   正确格式示例
     * @return 校验通过后的 trim 后值
     */
    private static String requirePattern(Pattern pattern, String value, String fieldName, String example) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, fieldName + "不能为空");
        }
        String normalized = value.trim();
        if (!pattern.matcher(normalized).matches()) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR,
                fieldName + "格式不正确：" + value + "，正确格式如 " + example);
        }
        return normalized;
    }

}
