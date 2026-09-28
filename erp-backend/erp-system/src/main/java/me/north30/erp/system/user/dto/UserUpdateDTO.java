package me.north30.erp.system.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 修改用户请求 DTO（接口文档 5.1.4，入参同 5.1.3 但不修改 username/userCode/password/roleIds）。
 * <p>带 version 乐观锁条件更新，冲突返回数据冲突错误。</p>
 */
public record UserUpdateDTO(

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

    @Size(max = 500, message = "备注长度不能超过 500")
    String remark,

    @NotNull(message = "乐观锁版本号不能为空")
    Integer version
) {
}
