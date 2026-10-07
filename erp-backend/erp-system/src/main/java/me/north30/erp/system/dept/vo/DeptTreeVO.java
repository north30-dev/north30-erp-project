package me.north30.erp.system.dept.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 组织部门树响应 VO（接口文档 5.4.1，children 递归）。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeptTreeVO(

    /** 组织 ID（sys_dept.id） */
    Long id,

    /** 组织编码（如 ORG001） */
    String deptCode,

    /** 组织名称 */
    String deptName,

    /** 上级组织 ID（0 为顶级） */
    Long parentId,

    /** 类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间 */
    Integer deptType,

    /** 层级（1 为顶级，上限 5 级） */
    Integer deptLevel,

    /** 祖级路径（如 0,1,5） */
    String ancestors,

    /** 负责人 */
    String leader,

    /** 联系电话 */
    String phone,

    /** 显示顺序 */
    Integer deptSort,

    /** 状态 0-停用 1-启用 */
    Integer status,

    /** 下级组织（递归结构，无子节点时为 null） */
    List<DeptTreeVO> children
) {
}
