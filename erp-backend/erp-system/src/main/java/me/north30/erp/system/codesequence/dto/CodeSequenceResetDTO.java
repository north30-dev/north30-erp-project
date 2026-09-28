package me.north30.erp.system.codesequence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 重置编号流水请求 DTO（接口文档 5.7.2，仅允许向下重置至未使用区间）。
 */
public record CodeSequenceResetDTO(

    @NotBlank(message = "单据类型不能为空")
    @Size(max = 30, message = "单据类型长度不能超过 30")
    String bizType,

    @NotBlank(message = "期间不能为空")
    @Pattern(regexp = "^\\d{6}$", message = "期间须为 yyyyMM 格式")
    String period,

    @NotNull(message = "目标流水号不能为空")
    Integer currentNo,

    @NotBlank(message = "重置原因不能为空")
    @Size(max = 200, message = "重置原因长度不能超过 200")
    String reason
) {
}
