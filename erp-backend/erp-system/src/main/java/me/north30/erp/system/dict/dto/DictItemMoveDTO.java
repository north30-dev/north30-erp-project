package me.north30.erp.system.dict.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 字典项上移/下移请求 DTO（POST /api/system/dict-items/{id}/move，08 文档阶段四排序统一）。
 *
 * @param direction 移动方向 up-上移 down-下移
 */
public record DictItemMoveDTO(

    /** 移动方向 up-上移 down-下移 */
    @NotBlank(message = "移动方向不能为空")
    @Pattern(regexp = "up|down", message = "移动方向仅支持 up/down")
    String direction
) {
}
