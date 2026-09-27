package me.north30.erp.system.core.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 用户-角色关联表（sys_user_role，多角色取权限并集）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_role")
public class SysUserRole extends BaseEntity {

    /** 用户 ID（sys_user.id） */
    private Long userId;

    /** 角色 ID（sys_role.id） */
    private Long roleId;
}
