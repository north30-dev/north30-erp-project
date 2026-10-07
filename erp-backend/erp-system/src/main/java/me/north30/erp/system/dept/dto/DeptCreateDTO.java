package me.north30.erp.system.dept.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增组织部门请求 DTO（接口文档 5.4.2）。
 *
 * @param deptCode  组织编码
 * @param deptName  组织名称
 * @param parentId  上级组织 ID（0 为顶级）
 * @param deptType  组织类型 1-集团 2-生产基地 3-销售分公司 4-部门 5-车间
 * @param leader    负责人
 * @param phone     联系电话
 * @param deptSort  显示顺序
 * @param status    状态 0-停用 1-启用
 * @param remark    备注
 */
public record DeptCreateDTO(

    @NotBlank(message = "组织编码不能为空")
    @Size(max = 50, message = "组织编码长度不能超过 50")
    String deptCode,

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 100, message = "组织名称长度不能超过 100")
    String deptName,

    @NotNull(message = "上级组织不能为空")
    Long parentId,

    @NotNull(message = "组织类型不能为空")
    Integer deptType,

    @Size(max = 50, message = "负责人长度不能超过 50")
    String leader,

    @Size(max = 30, message = "联系电话长度不能超过 30")
    String phone,

    Integer deptSort,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
