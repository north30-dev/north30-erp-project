package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 新增字典项请求 DTO（接口文档 5.5.6，dict_type+item_value+lang 唯一）。
 */
public record DictItemCreateDTO(

    @NotBlank(message = "字典类型编码不能为空")
    @Size(max = 100, message = "字典类型编码长度不能超过 100")
    String dictType,

    @NotBlank(message = "字典项标签不能为空")
    @Size(max = 200, message = "字典项标签长度不能超过 200")
    String itemLabel,

    @NotBlank(message = "字典项值不能为空")
    @Size(max = 100, message = "字典项值长度不能超过 100")
    String itemValue,

    @Size(max = 10, message = "语言编码长度不能超过 10")
    String lang,

    Integer itemSort,

    @Size(max = 100, message = "标签样式长度不能超过 100")
    String cssClass,

    Integer isDefault,

    String extJson,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
