package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.MenuCreateDTO;
import me.north30.erp.system.core.dto.MenuTreeQueryDTO;
import me.north30.erp.system.core.dto.MenuUpdateDTO;
import me.north30.erp.system.core.service.MenuManageService;
import me.north30.erp.system.core.vo.DeleteResultVO;
import me.north30.erp.system.core.vo.MenuCreatedVO;
import me.north30.erp.system.core.vo.MenuNodeVO;
import me.north30.erp.system.core.vo.MenuUpdatedVO;
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
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class SysMenuController {

    private final MenuManageService menuManageService;

    /**
     * 5.3.1 菜单树查询（一次查全部再内存组树）。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:menu:list')")
    public Result<List<MenuNodeVO>> tree(MenuTreeQueryDTO query) {
        return Result.success(menuManageService.tree(query));
    }

    /**
     * 5.3.2 新增菜单。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:create')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    public Result<MenuCreatedVO> create(@Valid @RequestBody MenuCreateDTO dto) {
        return Result.success(menuManageService.create(dto));
    }

    /**
     * 5.3.3 修改菜单。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:update')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public Result<MenuUpdatedVO> update(@PathVariable Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        return Result.success(menuManageService.update(id, dto));
    }

    /**
     * 5.3.4 删除菜单。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    public Result<DeleteResultVO> delete(@PathVariable Long id) {
        return Result.success(menuManageService.delete(id));
    }
}
