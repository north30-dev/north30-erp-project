package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.RoleAssignMenuDTO;
import me.north30.erp.system.core.dto.RoleCreateDTO;
import me.north30.erp.system.core.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.core.dto.RolePageQueryDTO;
import me.north30.erp.system.core.dto.RoleUpdateDTO;
import me.north30.erp.system.core.service.RoleManageService;
import me.north30.erp.system.core.vo.DeleteResultVO;
import me.north30.erp.system.core.vo.RoleCreatedVO;
import me.north30.erp.system.core.vo.RoleDataScopeVO;
import me.north30.erp.system.core.vo.RoleMenuAssignedVO;
import me.north30.erp.system.core.vo.RoleUpdatedVO;
import me.north30.erp.system.core.vo.RoleVO;
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
@RestController
@RequestMapping("/api/system/roles")
public class SysRoleController {

    private final RoleManageService roleManageService;

    public SysRoleController(RoleManageService roleManageService) {
        this.roleManageService = roleManageService;
    }

    /**
     * 5.2.1 角色分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public Result<PageResult<RoleVO>> page(RolePageQueryDTO query) {
        return Result.success(roleManageService.page(query));
    }

    /**
     * 5.2.2 新增角色。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:create')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    public Result<RoleCreatedVO> create(@Valid @RequestBody RoleCreateDTO dto) {
        return Result.success(roleManageService.create(dto));
    }

    /**
     * 5.2.3 修改角色。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public Result<RoleUpdatedVO> update(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        return Result.success(roleManageService.update(id, dto));
    }

    /**
     * 5.2.4 删除角色。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(roleManageService.delete(id));
    }

    /**
     * 5.2.5 分配角色菜单权限（全量覆盖）。
     */
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assignmenu')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public Result<RoleMenuAssignedVO> assignMenus(@PathVariable Long id, @Valid @RequestBody RoleAssignMenuDTO dto) {
        return Result.success(roleManageService.assignMenus(id, dto));
    }

    /**
     * 5.2.6 配置角色数据范围（全量覆盖）。
     */
    @PutMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('system:role:datascope')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public Result<RoleDataScopeVO> saveDataScopes(@PathVariable Long id,
                                                  @Valid @RequestBody RoleDataScopeSaveDTO dto) {
        return Result.success(roleManageService.saveDataScopes(id, dto));
    }

    /**
     * 5.2.7 查询角色数据范围。
     */
    @GetMapping("/{id}/data-scopes")
    @PreAuthorize("hasAuthority('system:role:detail')")
    public Result<RoleDataScopeVO> listDataScopes(@PathVariable Long id) {
        return Result.success(roleManageService.listDataScopes(id));
    }
}
