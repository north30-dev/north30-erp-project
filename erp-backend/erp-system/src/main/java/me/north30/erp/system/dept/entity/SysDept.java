package me.north30.erp.system.dept.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemManageErrorCode;

/**
 * 组织/部门表（sys_dept）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_dept")
public class SysDept extends BaseEntity {

    /** 顶级组织父 ID */
    private static final long ROOT_PARENT_ID = 0L;

    /** 组织层级上限（接口文档 18027） */
    private static final int MAX_DEPT_LEVEL = 5;

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

    /**
     * 变更上级组织并重算层级与祖级路径（newParent 为 null 表示顶级）。
     * <p>仅校验/修改自身状态：层级超过 {@value #MAX_DEPT_LEVEL} 上限抛 18027。</p>
     *
     * @param newParent 新上级组织实体（顶级传 null）
     */
    public void applyParent(SysDept newParent) {
        if (newParent == null) {
            this.parentId = ROOT_PARENT_ID;
            this.deptLevel = 1;
            this.ancestors = "0";
            return;
        }
        int level = newParent.getDeptLevel() + 1;
        if (level > MAX_DEPT_LEVEL) {
            throw new BusinessException(SystemManageErrorCode.DEPT_LEVEL_EXCEED);
        }
        this.parentId = newParent.getId();
        this.deptLevel = level;
        this.ancestors = newParent.getAncestors() + "," + newParent.getId();
    }
}
