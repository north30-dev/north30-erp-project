package me.north30.erp.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 角色表（sys_role）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 角色编码（如 admin、sales_manager） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 显示顺序 */
    private Integer roleSort;

    /** 数据范围 1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private Integer dataScope;

    /** 是否内置角色 0-否 1-是（内置不可删） */
    private Integer isBuiltin;

    /** 状态 0-停用 1-启用 */
    private Integer status;
}
