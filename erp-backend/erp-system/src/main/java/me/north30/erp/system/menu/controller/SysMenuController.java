package me.north30.erp.system.menu.controller;

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
import me.north30.erp.common.result.Result;
import me.north30.erp.system.menu.dto.MenuCreateDTO;
import me.north30.erp.system.menu.dto.MenuMoveDTO;
import me.north30.erp.system.menu.dto.MenuStatusDTO;
import me.north30.erp.system.menu.dto.MenuTreeQueryDTO;
import me.north30.erp.system.menu.dto.MenuUpdateDTO;
import me.north30.erp.system.menu.service.SysMenuApplicationService;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.menu.vo.MenuCreatedVO;
import me.north30.erp.system.menu.vo.MenuNodeVO;
import me.north30.erp.system.menu.vo.MenuStatusVO;
import me.north30.erp.system.menu.vo.MenuUpdatedVO;
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

import java.util.List;

/**
 * 菜单与权限点管理接口（/api/system/menus，接口文档 5.3 共 4 个接口）。
 * <p>权限点经 @PreAuthorize 校验（需启用 @EnableMethodSecurity）；写操作统一记录审计日志。</p>
 */
@Tag(name = "菜单管理", description = "/api/system/menus，接口文档 5.3")
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18022, "message": "权限标识已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysMenuApplicationService sysMenuApplicationService;

    /**
     * 5.3.1 菜单树查询（一次查全部再内存组树）。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    @Operation(summary = "菜单树查询", description = "权限点 system:menu:list；一次查全部后内存组树")
    public Result<List<MenuNodeVO>> tree(@ParameterObject MenuTreeQueryDTO query) {
        return Result.success(sysMenuApplicationService.tree(query));
    }

    /**
     * 5.3.2 新增菜单。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    @Operation(summary = "新增菜单", description = "权限点 system:menu:create；父子类型配对校验（按钮仅可挂菜单下），深度上限 4 级")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 菜单类型/类型必填字段/格式非法/18019 上级菜单不存在/18022 权限标识已存在/18023 菜单层级超过上限）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MenuCreatedVO> create(@Valid @RequestBody MenuCreateDTO dto) {
        return Result.success(sysMenuApplicationService.create(dto));
    }

    /**
     * 5.3.3 修改菜单。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "修改菜单", description = "权限点 system:menu:update；父级不得指向自身或自身下级（防环），乐观锁按 version 校验")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10001 菜单类型/类型必填字段/格式非法/存在子节点禁改类型/18019 菜单不存在/18022 权限标识已存在/18023 菜单层级超过上限/10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MenuUpdatedVO> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        return Result.success(sysMenuApplicationService.update(id, dto));
    }

    /**
     * 变更菜单状态（启用/停用，08 文档阶段四状态开关统一）。
     */
    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:menu:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "变更菜单状态", description = "权限点 system:menu:update；内置菜单（isSystem=1）及其子树不可停用（18039），乐观锁冲突返回 409/10601")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18019 菜单不存在/18039 内置菜单不可停用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MenuStatusVO> changeStatus(@PathVariable Long id, @Valid @RequestBody MenuStatusDTO dto) {
        return Result.success(sysMenuApplicationService.changeStatus(id, dto));
    }

    /**
     * 菜单上移/下移（08 文档阶段四排序统一）。
     */
    @PostMapping("/{id}/move")
    @PreAuthorize("hasAuthority('system:menu:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    @Operation(summary = "菜单上移/下移", description = "权限点 system:menu:update；同级规范化重排（10,20,30…）后相邻交换，乐观锁冲突返回 409/10601，已处于顶部/底部返回 422/10001")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18019 菜单不存在/10001 无法移动）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<Void> move(@PathVariable Long id, @Valid @RequestBody MenuMoveDTO dto) {
        sysMenuApplicationService.move(id, dto);
        return Result.success();
    }

    /**
     * 5.3.4 删除菜单。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    @Operation(summary = "删除菜单", description = "权限点 system:menu:delete；逻辑删除，删除前校验子节点与角色引用")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18019 菜单不存在/18020 存在子节点不可删除/18021 已被角色引用不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(sysMenuApplicationService.delete(id));
    }
}
