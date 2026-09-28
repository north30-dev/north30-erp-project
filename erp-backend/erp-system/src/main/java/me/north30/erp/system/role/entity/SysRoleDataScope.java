package me.north30.erp.system.role.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.north30.erp.common.mybatis.BaseEntity;

/**
 * 角色数据权限范围配置表（sys_role_data_scope，SYS-04：业务对象 + 过滤维度 + 数据范围 + 字段掩码）。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role_data_scope")
public class SysRoleDataScope extends BaseEntity {

    /** 角色 ID（sys_role.id） */
    private Long roleId;

    /** 业务对象 SALES_ORDER/PURCHASE_ORDER/WORK_ORDER/INVENTORY */
    private String bizObject;

    /** 过滤维度 CREATOR/CUSTOMER_OWNER/BUYER/PROD_ORG/WAREHOUSE */
    private String filterDimension;

    /** 数据范围 1-全部 2-本组织及下级 3-本组织 4-本部门及下级 5-本部门 6-仅本人 9-自定义 */
    private Integer scopeType;

    /** 自定义勾选组织 ID 集合（逗号分隔） */
    private String deptIds;

    /** 自定义勾选人员 ID 集合（逗号分隔） */
    private String userIds;

    /** 字段级掩码 0-不掩码 1-金额掩码 2-银行账号掩码 */
    private Integer fieldMask;
}
