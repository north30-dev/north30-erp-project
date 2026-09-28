package me.north30.erp.system.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 组织/部门表（sys_dept）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 组织编码（如 ORG001） */
    private String deptCode;

    /** 组织名称 */
    private String deptName;

    /** 上级组织 ID（0 为顶级） */
    private Long parentId;

    /** 类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间 */
    private Integer deptType;

    /** 层级（1 为顶级） */
    private Integer deptLevel;

    /** 祖级路径（如 0,1,5，用于"及下级"数据权限） */
    private String ancestors;

    /** 负责人 */
    private String leader;

    /** 联系电话 */
    private String phone;

    /** 显示顺序 */
    private Integer deptSort;

    /** 状态 0-停用 1-启用 */
    private Integer status;
}
