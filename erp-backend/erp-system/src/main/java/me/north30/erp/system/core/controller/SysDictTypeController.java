package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.DictTypeCreateDTO;
import me.north30.erp.system.core.dto.DictTypeQueryDTO;
import me.north30.erp.system.core.dto.DictTypeUpdateDTO;
import me.north30.erp.system.core.service.SysDictTypeService;
import me.north30.erp.system.core.vo.DictTypeVO;
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
 * 数据字典类型管理接口（/api/system/dict-types，接口文档 5.5.1-5.5.4，33-36 号接口）。
 */
@RestController
@RequestMapping("/api/system/dict-types")
public class SysDictTypeController {

    private final SysDictTypeService sysDictTypeService;

    public SysDictTypeController(SysDictTypeService sysDictTypeService) {
        this.sysDictTypeService = sysDictTypeService;
    }

    /**
     * 5.5.1 字典类型分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<DictTypeVO>> page(DictTypeQueryDTO query) {
        return Result.success(sysDictTypeService.page(query));
    }

    /**
     * 5.5.2 新增字典类型。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    public Result<MutationVO> create(@Valid @RequestBody DictTypeCreateDTO dto) {
        return Result.success(sysDictTypeService.create(dto));
    }

    /**
     * 5.5.3 修改字典类型。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    public Result<MutationVO> update(@PathVariable Long id, @Valid @RequestBody DictTypeUpdateDTO dto) {
        return Result.success(sysDictTypeService.update(id, dto));
    }

    /**
     * 5.5.4 删除字典类型。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<MutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDictTypeService.delete(id));
    }
}
