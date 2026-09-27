package me.north30.erp.system.core.dto;

/**
 * 用户分页查询/导出请求 DTO（接口文档 5.1.1/5.1.9，GET 查询参数绑定）。
 */
public record UserQueryDTO(
    String username,
    String realName,
    String userCode,
    Long deptId,
    Integer status,
    Integer pageNum,
    Integer pageSize
) {
}
