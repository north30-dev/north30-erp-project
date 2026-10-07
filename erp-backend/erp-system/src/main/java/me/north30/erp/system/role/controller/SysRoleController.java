package me.north30.erp.system.role.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.role.service.RoleManageService;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleUpdatedVO;
import me.north30.erp.system.role.vo.RoleVO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 角色管理接口（/api/system/roles，接口文档 5.2 共 7 个接口）。
 * <p>权限点经 @PreAuthorize 校验（需启用 @EnableMethodSecurity）；写操作统一记录审计日志。</p>
 */
@Tag(name = "角色管理", description = "/api/system/roles，接口文档 5.2")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class SysRoleController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18015, "message": "角色编码已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final RoleManageService roleManageService;
    /**
     * 5.2.1 角色分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    @Operation(summary = "角色分页查询", description = "权限点 system:role:list；userCount 为派生字段（按页批量统计）")
    public Result<PageResult<RoleVO>> page(@ParameterObject RolePageQueryDTO query) {
        return Result.success(roleManageService.page(query));
    }

    /**
     * 5.2.2 新增角色。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "新增角色", description = "权限点 system:role:create；新建角色均为非内置（isBuiltin=0）")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18015 角色编码已存在/18018 数据范围配置非法）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<RoleCreatedVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        return Result.success(roleManageService.create(dto));
    }

    /**
     * 5.2.3 修改角色。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改角色", description = "权限点 system:role:update；role_code 不可修改，version 不一致返回乐观锁冲突")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18014 角色不存在/18018 数据范围配置非法/10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<RoleUpdatedVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        return Result.success(roleManageService.update(id, dto));
    }

    /**
     * 5.2.4 删除角色。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    @Operation(summary = "删除角色", description = "权限点 system:role:delete；逻辑删除，级联清理菜单授权与数据范围配置")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18014 角色不存在/18017 内置角色不可删除/18016 角色已分配用户不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(roleManageService.delete(id));
    }

    /**
     * 5.2.5 分配角色菜单权限（全量覆盖）。
     */
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assignmenu')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "分配角色菜单权限", description = "权限点 system:role:assignmenu；全量覆盖语义，返回权限点计数")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18014 角色不存在/18019 菜单不存在或已删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<RoleMenuAssignedVO> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleAssignMenuDTO dto) {
        return Result.success(roleManageService.assignMenus(id, dto));
    }

    /**
     * 5.2.6 配置角色数据范围（全量覆盖）。
     */
    @PutMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('system:role:datascope')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "配置角色数据范围", description = "权限点 system:role:datascope；全量覆盖语义，自定义范围须至少勾选组织或人员")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18014 角色不存在/18018 数据范围配置非法）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<RoleDataScopeVO> saveDataScopes(@PathVariable Long id,
                                                  @Valid @RequestBody RoleDataScopeSaveDTO dto) {
        return Result.success(roleManageService.saveDataScopes(id, dto));
    }

    /**
     * 5.2.7 查询角色数据范围。
     */
    @GetMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('system:role:detail')")
    @Operation(summary = "查询角色数据范围", description = "权限点 system:role:detail；角色不存在返回 422/18014")
    public Result<RoleDataScopeVO> listDataScopes(@PathVariable Long id) {
        return Result.success(roleManageService.listDataScopes(id));
    }
}
