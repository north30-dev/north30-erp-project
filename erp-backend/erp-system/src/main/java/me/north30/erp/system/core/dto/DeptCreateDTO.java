package me.north30.erp.system.core.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增组织部门请求 DTO（接口文档 5.4.2）。
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
