package me.north30.erp.system.dict.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.dict.dto.DictTypeCreateDTO;
import me.north30.erp.system.dict.dto.DictTypeQueryDTO;
import me.north30.erp.system.dict.dto.DictTypeUpdateDTO;
import me.north30.erp.system.dict.service.SysDictTypeService;
import me.north30.erp.system.dict.vo.DictTypeVO;
import me.north30.erp.system.common.vo.MutationVO;
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
 * 数据字典类型管理接口（/api/system/dict-types，接口文档 5.5.1-5.5.4，33-36 号接口）。
 */
@Tag(name = "数据字典类型", description = "/api/system/dict-types，接口文档 5.5.1-5.5.4")
@RestController
@RequestMapping("/api/system/dict-types")
@RequiredArgsConstructor
public class SysDictTypeController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18029, "message": "字典类型已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysDictTypeService sysDictTypeService;

    /**
     * 5.5.1 字典类型分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:list')")
    @Operation(summary = "字典类型分页查询", description = "权限点 system:dict:list")
    public Result<PageResult<DictTypeVO>> page(@ParameterObject @Valid DictTypeQueryDTO query) {
        return Result.success(sysDictTypeService.page(query));
    }

    /**
     * 5.5.2 新增字典类型。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    @Operation(summary = "新增字典类型", description = "权限点 system:dict:create")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18029 字典类型已存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> create(@Valid @RequestBody DictTypeCreateDTO dto) {
        return Result.success(sysDictTypeService.create(dto));
    }

    /**
     * 5.5.3 修改字典类型。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @Operation(summary = "修改字典类型", description = "权限点 system:dict:update")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18028 字典类型不存在、10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> update(@PathVariable Long id, @Valid @RequestBody DictTypeUpdateDTO dto) {
        return Result.success(sysDictTypeService.update(id, dto));
    }

    /**
     * 5.5.4 删除字典类型。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @Operation(summary = "删除字典类型", description = "权限点 system:dict:delete")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18028 字典类型不存在、18030 字典类型下存在字典项不可删除）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDictTypeService.delete(id));
    }
}
