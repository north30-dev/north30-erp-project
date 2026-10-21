package me.north30.erp.system.config.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 系统参数分页查询参数（接口文档 5.6.1）。
 *
 * @param configKey   参数键名（模糊）
 * @param configName  参数名称（模糊）
 * @param configGroup 参数分组（精确）
 * @param status      状态 0-停用 1-启用
 * @param pageNum     页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize    每页条数（1-200）
 */
public record ConfigQueryDTO(
    String configKey,
    String configName,
    String configGroup,
    Integer status,
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
