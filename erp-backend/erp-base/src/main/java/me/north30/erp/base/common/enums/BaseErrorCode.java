package me.north30.erp.base.common.enums;

import lombok.Getter;
import me.north30.erp.common.exception.ErrorCode;

/**
 * 基础数据域错误码（11001-11999，接口文档 2.3，模块内唯一来源）。
 * <p>号段一次预留六个子域（规划文档第三章），新码在对应子域区间内追加、不得插入已定义码之间：</p>
 * <ul>
 *     <li>11001-11049：物料编码与分类</li>
 *     <li>11050-11099：物料属性与状态</li>
 *     <li>11101-11159：BOM 结构与闭环</li>
 *     <li>11160-11199：BOM 版本与审核</li>
 *     <li>11201-11249：客户</li>
 *     <li>11250-11299：供应商</li>
 * </ul>
 * <p>本期仅落地第 4 周物料部分所需错误码（文案对照接口文档 2.3 提示文案列）；
 * 不得使用 system 域 18xxx 段。消息中 {param} 为占位符，抛出时以自定义消息填充。</p>
 */
@Getter
public enum BaseErrorCode implements ErrorCode {

    // ==================== 物料编码与分类（11001-11049） ====================

    /** 物料编码已存在（新增/导入物料编码重复） */
    MATERIAL_CODE_EXISTS(11001, "物料编码 {materialCode} 已存在"),

    /** 物料不存在（按 ID/编码查询无结果） */
    MATERIAL_NOT_FOUND(11002, "物料 {materialCode} 不存在"),

    /** 物料已停用（停用物料被用于建单/BOM 子件） */
    MATERIAL_DISABLED(11003, "物料 {materialCode} 已停用，不可使用"),

    /** 物料编码与分类不匹配（编码前 4 位与所选二级分类编码不一致） */
    MATERIAL_CODE_CATEGORY_MISMATCH(11004, "物料编码前 4 位须与分类编码一致（当前分类 {categoryCode}）"),

    /** 物料必须挂载叶子分类（category_id 指向非叶子分类） */
    MATERIAL_CATEGORY_NOT_LEAF(11005, "物料分类须选择叶子节点"),

    /** 分类存在子节点不可删除（删除含下级的物料分类） */
    CATEGORY_HAS_CHILDREN(11006, "分类 {categoryName} 存在下级分类，不可删除"),

    /** 分类编码已存在（新增/修改分类编码重复） */
    CATEGORY_CODE_EXISTS(11007, "分类编码 {categoryCode} 已存在"),

    /** 物料重复（名称+规格+单位+品牌四要素与既有物料一致） */
    MATERIAL_DUPLICATED(11008, "已存在相同物料（{materialCode}），请确认是否重复"),

    /** 物料已启用不可删除（删除已启用/已审核物料） */
    MATERIAL_ENABLED_UNDELETABLE(11009, "物料 {materialCode} 已启用，不可删除"),

    /** 物料未审核（未审核直接启用） */
    MATERIAL_NOT_AUDITED(11010, "物料 {materialCode} 未审核，不可启用"),

    /** 物料已被引用不可删除（物料已被 BOM/订单/流水引用） */
    MATERIAL_REFERENCED(11011, "物料 {materialCode} 已被业务单据引用，不可删除"),

    /** 计价方法不可变更（本期仅实现移动加权平均） */
    MATERIAL_COST_METHOD_IMMUTABLE(11012, "计价方法本期仅支持移动加权平均，不可变更"),

    /** 物料编码流水已用尽（分类下 6 位流水超过 999999） */
    MATERIAL_CODE_EXHAUSTED(11014, "分类 {categoryCode} 下物料编码流水已用尽"),

    /** 旧码映射已存在（old_code + source_dept 重复） */
    CODE_MAP_EXISTS(11015, "旧物料编码 {oldCode}（{sourceDept}）映射已存在"),

    /** 物料计量单位非法（单位不在字典 unit 取值内） */
    MATERIAL_UNIT_INVALID(11016, "计量单位 {unit} 非法，请从字典中选择"),

    /** 物料分类已停用（物料挂载到停用分类） */
    MATERIAL_CATEGORY_DISABLED(11017, "物料分类 {categoryName} 已停用"),

    // ==================== 物料属性与状态（11050-11099，本期未定义，11050 起追加） ====================

    // ==================== BOM 结构与闭环（11101-11159，本期未定义，11101 起追加） ====================

    // ==================== BOM 版本与审核（11160-11199，本期未定义，11160 起追加） ====================

    // ==================== 客户（11201-11249，本期未定义，11201 起追加） ====================

    // ==================== 供应商（11250-11299，本期未定义，11250 起追加） ====================
    ;

    /** 业务状态码 */
    private final int code;

    /** 默认提示消息 */
    private final String message;

    BaseErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
