package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 修改字典项请求 DTO（接口文档 5.5.7，item_value 不可修改）。
 */
public record DictItemUpdateDTO(

    @Size(max = 200, message = "字典项标签长度不能超过 200")
    String itemLabel,

    Integer itemSort,

    @Size(max = 100, message = "标签样式长度不能超过 100")
    String cssClass,

    Integer isDefault,

    String extJson,

    Integer status,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "版本号不能为空")
    Integer version
) {
}
