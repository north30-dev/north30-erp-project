package me.north30.erp.system.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

import java.time.LocalDateTime;

/**
 * 用户表（sys_user）：口令为 BCrypt 密文。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    /** 用户编号（HR 工号，全局唯一） */
    private String userCode;

    /** 登录用户名（全局唯一） */
    private String username;

    /** 口令（BCrypt 强度 10 密文，禁止明文） */
    private String password;

    /** 姓名 */
    private String realName;

    /** 所属组织/部门（sys_dept.id，逻辑关联） */
    private Long deptId;

    /** 可访问仓库 ID 集合（逗号分隔，数据权限用） */
    private String warehouseIds;

    /** 联系电话 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 性别 0-未知 1-男 2-女 */
    private Integer gender;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /** 是否超级管理员 0-否 1-是 */
    private Integer isAdmin;

    /** 口令最后修改时间（90 天有效期基准） */
    private LocalDateTime passwordUpdateTime;

    /** 最后登录时间 */
    private LocalDateTime lastLoginTime;

    /** 最后登录 IP */
    private String lastLoginIp;

    /** 连续登录失败次数（阈值 5 次） */
    private Integer loginFailCount;

    /** 账号锁定到期时间（锁定 30 分钟） */
    private LocalDateTime lockUntil;
}
