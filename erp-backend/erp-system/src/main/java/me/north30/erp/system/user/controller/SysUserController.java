package me.north30.erp.system.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.service.UserManagementService;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserResetPasswordVO;
import me.north30.erp.system.user.vo.UserStatusVO;
import me.north30.erp.system.user.vo.UserVO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户管理接口（/api/system/users，接口文档第五章 5.1 用户管理）。
 * <p>权限点通过 @PreAuthorize 方法级鉴权（SecurityConfig 已启用 @EnableMethodSecurity）；
 * 写操作的业务校验与审计落库在 UserManagementService 中完成。</p>
 */
@Tag(name = "用户管理", description = "/api/system/users，接口文档 5.1")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18006, "message": "用户名已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    /** 导出文件名时间后缀格式 */
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserManagementService userManagementService;

    /**
     * 5.1.1 用户分页查询（system:user:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    @Operation(summary = "用户分页查询", description = "权限点 system:user:list")
    public Result<PageResult<UserVO>> page(@ParameterObject @Valid UserQueryDTO query) {
        return Result.success(userManagementService.page(query));
    }

    /**
     * 5.1.9 用户列表导出（system:user:export）。
     * <p>简化口径：CSV 文件流（UTF-8 BOM），列与列表页一致。</p>
     */
    @Operation(summary = "用户列表导出", description = "权限点 system:user:export；返回 CSV 文件流（UTF-8 BOM），文件名在 Content-Disposition 头")
    @ApiResponse(responseCode = "200", description = "CSV 文件流",
        content = @Content(mediaType = "text/csv"))
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('system:user:export')")
    public ResponseEntity<byte[]> export(@ParameterObject @Valid UserQueryDTO query) {
        byte[] csv = userManagementService.exportCsv(query);
        String filename = "用户列表_" + FILENAME_FORMATTER.format(LocalDateTime.now()) + ".csv";
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
            .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
            .body(csv);
    }

    /**
     * 5.1.2 用户详情（system:user:detail）。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:detail')")
    @Operation(summary = "用户详情", description = "权限点 system:user:detail；不存在返回 422/18005")
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        return Result.success(userManagementService.getDetail(id));
    }

    /**
     * 5.1.3 新增用户（system:user:create）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    @Operation(summary = "新增用户", description = "权限点 system:user:create；初始口令 BCrypt 加密落库")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18006 用户名存在/18007 编号存在/18009 口令复杂度不足）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userManagementService.create(dto));
    }

    /**
     * 5.1.4 修改用户（system:user:update，乐观锁）。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    @Operation(summary = "修改用户", description = "权限点 system:user:update；version 不一致返回 422（乐观锁冲突）")
    @ApiResponse(responseCode = "422", description = "业务校验失败（乐观锁冲突/用户不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userManagementService.update(id, dto));
    }

    /**
     * 5.1.5 删除用户（system:user:delete，逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @Operation(summary = "删除用户", description = "权限点 system:user:delete；逻辑删除，管理员不可删除")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18011 不允许删除管理员/18005 用户不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(userManagementService.delete(id));
    }

    /**
     * 5.1.6 启用/停用用户（system:user:update，停用即失效全部在线会话）。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    @Operation(summary = "启用/停用用户", description = "权限点 system:user:update；停用即失效全部在线会话")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18005 用户不存在/18010 不允许停用管理员）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<UserStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        return Result.success(userManagementService.changeStatus(id, dto));
    }

    /**
     * 5.1.7 重置用户密码（system:user:resetpwd）。
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetpwd')")
    @Operation(summary = "重置用户密码", description = "权限点 system:user:resetpwd")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18005 用户不存在/18009 口令复杂度不足）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<UserResetPasswordVO> resetPassword(@PathVariable Long id,
                                                     @Valid @RequestBody UserResetPasswordDTO dto) {
        return Result.success(userManagementService.resetPassword(id, dto));
    }

    /**
     * 5.1.8 分配用户角色（system:user:assignrole，全量覆盖语义）。
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assignrole')")
    @Operation(summary = "分配用户角色", description = "权限点 system:user:assignrole；全量覆盖语义")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18005 用户不存在/18014 角色不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<UserAssignRolesVO> assignRoles(@PathVariable Long id,
                                                 @Valid @RequestBody UserAssignRolesDTO dto) {
        return Result.success(userManagementService.assignRoles(id, dto));
    }
}
