package me.north30.erp.system.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 用户分页查询/导出请求 DTO（接口文档 5.1.1/5.1.9，GET 查询参数绑定）。
 *
 * @param username 用户名（模糊）
 * @param realName 真实姓名（模糊）
 * @param userCode 工号（模糊）
 * @param deptId   所属部门 ID
 * @param status   状态 0-停用 1-启用
 * @param pageNum  页码（从 1 开始，缺省由服务层兜底）
 * @param pageSize 每页条数（1-200）
 */
public record UserQueryDTO(
    String username,
    String realName,
    String userCode,
    Long deptId,
    Integer status,
    @Min(value = 1, message = "页码不能小于 1")
    Integer pageNum,
    @Min(value = 1, message = "每页条数不能小于 1")
    @Max(value = 200, message = "每页条数不能超过 200")
    Integer pageSize
) {
}
