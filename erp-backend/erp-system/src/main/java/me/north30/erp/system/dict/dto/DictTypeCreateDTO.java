package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 新增字典类型请求 DTO（接口文档 5.5.2）。
 *
 * @param dictType 字典类型编码（唯一，字母开头）
 * @param dictName 字典类型名称
 * @param status   状态 0-停用 1-启用
 * @param remark   备注
 */
public record DictTypeCreateDTO(

    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过 100")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型编码仅允许小写字母开头，由小写字母、数字与下划线组成")
    String dictType,

    @NotBlank(message = "字典类型名称不能为空")
    @Size(max = 100, message = "字典类型名称长度不能超过 100")
    String dictName,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
