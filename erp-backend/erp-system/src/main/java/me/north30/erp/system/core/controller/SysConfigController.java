package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.ConfigCreateDTO;
import me.north30.erp.system.core.dto.ConfigQueryDTO;
import me.north30.erp.system.core.dto.ConfigUpdateDTO;
import me.north30.erp.system.core.service.ISysConfigService;
import me.north30.erp.system.core.vo.ConfigUpdateVO;
import me.north30.erp.system.core.vo.ConfigVO;
import me.north30.erp.system.core.vo.MutationVO;
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
 * 系统参数管理接口（/api/system/configs，接口文档 5.6，41-44 号接口）。
 */
@RestController
@RequestMapping("/api/system/configs")
public class SysConfigController {

    private final ISysConfigService sysConfigService;

    public SysConfigController(ISysConfigService sysConfigService) {
        this.sysConfigService = sysConfigService;
    }

    /**
     * 5.6.1 系统参数分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:config:list')")
    public Result<PageResult<ConfigVO>> page(ConfigQueryDTO query) {
        return Result.success(sysConfigService.page(query));
    }

    /**
     * 5.6.2 新增系统参数。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:config:create')")
    public Result<MutationVO> create(@Valid @RequestBody ConfigCreateDTO dto) {
        return Result.success(sysConfigService.create(dto));
    }

    /**
     * 5.6.3 修改系统参数。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:update')")
    public Result<ConfigUpdateVO> update(@PathVariable Long id, @Valid @RequestBody ConfigUpdateDTO dto) {
        return Result.success(sysConfigService.update(id, dto));
    }

    /**
     * 5.6.4 删除系统参数。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:config:delete')")
    public Result<MutationVO> delete(@PathVariable Long id) {
        return Result.success(sysConfigService.delete(id));
    }
}
