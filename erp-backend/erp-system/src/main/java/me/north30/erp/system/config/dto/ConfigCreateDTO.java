package me.north30.erp.system.config.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 新增系统参数请求 DTO（接口文档 5.6.2，config_key 唯一）。
 */
public record ConfigCreateDTO(

    @NotBlank(message = "参数键不能为空")
    @Size(max = 100, message = "参数键长度不能超过 100")
    String configKey,

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 200, message = "参数名称长度不能超过 200")
    String configName,

    @NotBlank(message = "参数值不能为空")
    @Size(max = 500, message = "参数值长度不能超过 500")
    String configValue,

    @NotNull(message = "参数值类型不能为空")
    @Min(value = 1, message = "参数值类型仅支持 1-字符串 2-数字 3-布尔 4-JSON")
    @Max(value = 4, message = "参数值类型仅支持 1-字符串 2-数字 3-布尔 4-JSON")
    Integer valueType,

    @NotBlank(message = "参数分组不能为空")
    @Size(max = 50, message = "参数分组长度不能超过 50")
    String configGroup,

    Integer isSystem,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
