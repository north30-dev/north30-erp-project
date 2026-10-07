package me.north30.erp.system.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 新增用户请求 DTO（接口文档 5.1.3）。
 * <p>密码字段仅入站传输（WRITE_ONLY），不回显、不出现在响应与审计日志中（S-06）。</p>
 *
 * @param userCode     用户编号（≤50 字符）
 * @param username     用户名（≤50 字符）
 * @param password     初始密码（≤64 字符）
 * @param realName     真实姓名（≤50 字符）
 * @param deptId       所属部门 ID
 * @param warehouseIds 授权仓库 ID 集合
 * @param phone        联系电话（≤30 字符）
 * @param email        邮箱（≤100 字符）
 * @param gender       性别（0-未知 1-男 2-女）
 * @param status       状态（0-停用 1-启用）
 * @param roleIds      角色 ID 集合
 * @param remark       备注（≤500 字符）
 */
public record UserCreateDTO(

    @NotBlank(message = "用户编号不能为空")
    @Size(max = 50, message = "用户编号长度不能超过 50")
    String userCode,

    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    String username,

    @NotBlank(message = "初始密码不能为空")
    @Size(max = 64, message = "初始密码长度不能超过 64")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    String password,

    @NotBlank(message = "姓名不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50")
    String realName,

    Long deptId,

    List<Long> warehouseIds,

    @Size(max = 30, message = "联系电话长度不能超过 30")
    String phone,

    @Size(max = 100, message = "邮箱长度不能超过 100")
    @Email(message = "邮箱格式不正确")
    String email,

    @Min(value = 0, message = "性别取值仅支持 0-未知 1-男 2-女")
    @Max(value = 2, message = "性别取值仅支持 0-未知 1-男 2-女")
    Integer gender,

    @Min(value = 0, message = "状态取值仅支持 0-停用 1-启用")
    @Max(value = 1, message = "状态取值仅支持 0-停用 1-启用")
    Integer status,

    List<Long> roleIds,

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark
) {
}
