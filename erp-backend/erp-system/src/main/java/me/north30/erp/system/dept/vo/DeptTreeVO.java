package me.north30.erp.system.dept.vo;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 组织部门树响应 VO（接口文档 5.4.1，children 递归）。
 *
 * @param id        组织 ID（sys_dept.id）
 * @param deptCode  组织编码（如 ORG001）
 * @param deptName  组织名称
 * @param parentId  上级组织 ID（0 为顶级）
 * @param deptType  类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间
 * @param deptLevel 层级（1 为顶级，上限 5 级）
 * @param ancestors 祖级路径（如 0,1,5）
 * @param leader    负责人
 * @param phone     联系电话
 * @param deptSort  显示顺序
 * @param status    状态 0-停用 1-启用
 * @param children  下级组织（递归结构，无子节点时为 null）
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeptTreeVO(

    Long id,

    String deptCode,

    String deptName,

    Long parentId,

    Integer deptType,

    Integer deptLevel,

    String ancestors,

    String leader,

    String phone,

    Integer deptSort,

    Integer status,

    List<DeptTreeVO> children
) {
}
