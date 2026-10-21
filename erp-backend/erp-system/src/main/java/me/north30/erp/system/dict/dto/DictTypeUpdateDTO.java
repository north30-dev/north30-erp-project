package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改字典类型请求 DTO（接口文档 5.5.3，dict_type 不可修改）。
 *
 * @param dictName 字典类型名称
 * @param status   状态 0-停用 1-启用
 * @param remark   备注
 * @param version  乐观锁版本号
 */
public record DictTypeUpdateDTO(

    @NotBlank(message = "字典类型名称不能为空")
    @Size(max = 100, message = "字典类型名称长度不能超过 100")
    String dictName,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
