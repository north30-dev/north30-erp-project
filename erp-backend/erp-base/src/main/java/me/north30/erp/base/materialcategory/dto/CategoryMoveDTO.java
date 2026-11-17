package me.north30.erp.base.materialcategory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 物料分类上移/下移请求 DTO（POST /api/base/material-categories/{id}/move，spec 任务 2.2）。
 * <p>同级规范化重排（10,20,30…）后相邻交换；接口文档 6.1 未列 move 接口，
 * 按 spec 要求对齐组织部门 move 模式补齐。</p>
 *
 * @param direction 移动方向 up-上移 down-下移
 */
public record CategoryMoveDTO(

    /** 移动方向 up-上移 down-下移 */
    @NotBlank(message = "移动方向不能为空")
    @Pattern(regexp = "up|down", message = "移动方向仅支持 up/down")
    String direction
) {
}
