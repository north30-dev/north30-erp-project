package me.north30.erp.system.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改系统参数请求 DTO（接口文档 5.6.3，config_key 不可修改，按 valueType 校验新值）。
 */
public record ConfigUpdateDTO(

    @NotBlank(message = "参数值不能为空")
    @Size(max = 500, message = "参数值长度不能超过 500")
    String configValue,

    @Size(max = 200, message = "参数名称长度不能超过 200")
    String configName,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
