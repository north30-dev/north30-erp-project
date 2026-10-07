package me.north30.erp.system.role.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 角色分页查询请求 DTO（接口文档 5.2.1，分页规范见 1.4）。
 *
 * @param roleCode       角色编码（模糊）
 * @param roleName       角色名称（模糊）
 * @param status         状态 0-停用 1-启用
 * @param pageNum        页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize       每页条数（1-200）
 * @param orderBy        排序字段
 * @param orderDirection 排序方向 asc/desc
 */
public record RolePageQueryDTO(
    String roleCode,
    String roleName,
    Integer status,
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize,
    String orderBy,
    String orderDirection
) {
}
