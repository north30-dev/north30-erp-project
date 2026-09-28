package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.UserAssignRolesDTO;
import me.north30.erp.system.core.dto.UserCreateDTO;
import me.north30.erp.system.core.dto.UserQueryDTO;
import me.north30.erp.system.core.dto.UserResetPasswordDTO;
import me.north30.erp.system.core.dto.UserStatusDTO;
import me.north30.erp.system.core.dto.UserUpdateDTO;
import me.north30.erp.system.core.service.UserManagementService;
import me.north30.erp.system.core.vo.UserAssignRolesVO;
import me.north30.erp.system.core.vo.UserDeleteVO;
import me.north30.erp.system.core.vo.UserDetailVO;
import me.north30.erp.system.core.vo.UserResetPasswordVO;
import me.north30.erp.system.core.vo.UserStatusVO;
import me.north30.erp.system.core.vo.UserVO;
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
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class SysUserController {

    /** 导出文件名时间后缀格式 */
    private static final DateTimeFormatter FILENAME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final UserManagementService userManagementService;

    /**
     * 5.1.1 用户分页查询（system:user:list）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public Result<PageResult<UserVO>> page(UserQueryDTO query) {
        return Result.success(userManagementService.page(query));
    }

    /**
     * 5.1.9 用户列表导出（system:user:export）。
     * <p>简化口径：CSV 文件流（UTF-8 BOM），列与列表页一致。</p>
     */
    @GetMapping("/export")
    @PreAuthorize("hasAuthority('system:user:export')")
    public ResponseEntity<byte[]> export(UserQueryDTO query) {
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
    public Result<UserDetailVO> detail(@PathVariable Long id) {
        return Result.success(userManagementService.getDetail(id));
    }

    /**
     * 5.1.3 新增用户（system:user:create）。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:create')")
    public Result<Long> create(@Valid @RequestBody UserCreateDTO dto) {
        return Result.success(userManagementService.create(dto));
    }

    /**
     * 5.1.4 修改用户（system:user:update，乐观锁）。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<String> update(@PathVariable Long id, @Valid @RequestBody UserUpdateDTO dto) {
        return Result.success(userManagementService.update(id, dto));
    }

    /**
     * 5.1.5 删除用户（system:user:delete，逻辑删除）。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    public Result<UserDeleteVO> delete(@PathVariable Long id) {
        return Result.success(userManagementService.delete(id));
    }

    /**
     * 5.1.6 启用/停用用户（system:user:update，停用即失效全部在线会话）。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:update')")
    public Result<UserStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody UserStatusDTO dto) {
        return Result.success(userManagementService.changeStatus(id, dto));
    }

    /**
     * 5.1.7 重置用户密码（system:user:resetpwd）。
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasAuthority('system:user:resetpwd')")
    public Result<UserResetPasswordVO> resetPassword(@PathVariable Long id,
                                                     @Valid @RequestBody UserResetPasswordDTO dto) {
        return Result.success(userManagementService.resetPassword(id, dto));
    }

    /**
     * 5.1.8 分配用户角色（system:user:assignrole，全量覆盖语义）。
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assignrole')")
    public Result<UserAssignRolesVO> assignRoles(@PathVariable Long id,
                                                 @Valid @RequestBody UserAssignRolesDTO dto) {
        return Result.success(userManagementService.assignRoles(id, dto));
    }
}
