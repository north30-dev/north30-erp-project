package me.north30.erp.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.exception.BusinessException;
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
     * roleSort 显示顺序默认 0
     * isBuiltin 是否内置角色默认 0
     * status 状态默认 1
     * </p>
     */
    public void applyCreateDefault() {
        if (roleSort == null) {
            roleSort = 0;
        }
        if (isBuiltin == null) {
            isBuiltin = 0;
        }
        if (status == null) {
            status = 1;
        }
    }
}
