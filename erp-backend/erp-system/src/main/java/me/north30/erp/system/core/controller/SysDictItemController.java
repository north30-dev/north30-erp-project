package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.DictItemCreateDTO;
import me.north30.erp.system.core.dto.DictItemQueryDTO;
import me.north30.erp.system.core.dto.DictItemUpdateDTO;
import me.north30.erp.system.core.service.ISysDictItemService;
import me.north30.erp.system.core.vo.DictItemVO;
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
 * 数据字典项管理接口（/api/system/dict-items，接口文档 5.5.5-5.5.8，37-40 号接口）。
 */
@RestController
@RequestMapping("/api/system/dict-items")
public class SysDictItemController {

    private final ISysDictItemService sysDictItemService;

    public SysDictItemController(ISysDictItemService sysDictItemService) {
        this.sysDictItemService = sysDictItemService;
    }

    /**
     * 5.5.5 字典项查询（按字典类型过滤）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:list')")
    public Result<PageResult<DictItemVO>> page(DictItemQueryDTO query) {
        return Result.success(sysDictItemService.page(query));
    }

    /**
     * 5.5.6 新增字典项。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    public Result<MutationVO> create(@Valid @RequestBody DictItemCreateDTO dto) {
        return Result.success(sysDictItemService.create(dto));
    }

    /**
     * 5.5.7 修改字典项。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    public Result<MutationVO> update(@PathVariable Long id, @Valid @RequestBody DictItemUpdateDTO dto) {
        return Result.success(sysDictItemService.update(id, dto));
    }

    /**
     * 5.5.8 删除字典项。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    public Result<MutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDictItemService.delete(id));
    }
}
