package me.north30.erp.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.mybatis.BaseEntity;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.role.enums.DataScopeEnum;

/**
 * 角色表（sys_role）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends BaseEntity {

    /** 超级管理员角色编码（固定，三层角色模型顶层，不走菜单勾选） */
    public static final String ROLE_CODE_ADMIN = "ADMIN";

    /** 系统管理员内置角色编码（预勾选全部 /system 菜单，权限集可改） */
    public static final String ROLE_CODE_SYSTEM_ADMIN = "SYSTEM_ADMIN";

    /** 角色编码（如 admin、sales_manager） */
    private String roleCode;

    /** 角色名称 */
    private String roleName;

    /** 数据范围 1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private Integer dataScope;

    /** 是否内置角色 0-否 1-是（内置不可删） */
    private Integer isBuiltin;

    /** 状态 0-停用 1-启用 */
    private Integer status;

    /**
     * 变更状态：值域守卫（10001）+ 内置角色不可停用守卫（18017，08 文档 2.5 收进实体）。
     * <p>内置角色（isBuiltin=1）允许启用、拒绝停用。</p>
     *
     * @param targetStatus 目标状态 0-停用 1-启用
     */
    public void changeStatus(Integer targetStatus) {
        if (targetStatus == null || (targetStatus != 0 && targetStatus != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态值非法：" + targetStatus);
        }
        if (targetStatus == 0 && Integer.valueOf(1).equals(isBuiltin)) {
            throw new BusinessException(SystemErrorCode.ROLE_BUILTIN, "内置角色不可停用");
        }
        this.status = targetStatus;
    }

    /**
     * 变更数据范围档位，非法抛 18026。
     * <p>
     * 仅校验/修改自身状态。
     * </p>
     */
    public void changeDataScope(Integer dataScope) {
        
        if (!DataScopeEnum.isValid(dataScope)) {
            throw new BusinessException(SystemErrorCode.ROLE_DATA_SCOPE_INVALID,
                    "数据范围配置非法：data_scope=" + dataScope);
        }
        this.dataScope = dataScope;
    }

    /**
     * 应用创建默认值。
     * <p>
     * isBuiltin 是否内置角色默认 0
     * status 状态默认 1
     * </p>
     */
    public void applyCreateDefault() {
        if (isBuiltin == null) {
            isBuiltin = 0;
        }
        if (status == null) {
            status = 1;
        }
    }
}
