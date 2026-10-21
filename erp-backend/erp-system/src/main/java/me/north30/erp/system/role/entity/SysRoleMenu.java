package me.north30.erp.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 角色-菜单关联表（sys_role_menu）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_menu")
public class SysRoleMenu extends BaseEntity {

    /** 角色 ID（sys_role.id） */
    private Long roleId;

    /** 菜单 ID（sys_menu.id） */
    private Long menuId;
}
