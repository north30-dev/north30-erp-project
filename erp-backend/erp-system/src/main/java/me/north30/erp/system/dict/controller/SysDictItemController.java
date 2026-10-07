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
import me.north30.erp.system.dict.dto.DictItemCreateDTO;
import me.north30.erp.system.dict.dto.DictItemQueryDTO;
import me.north30.erp.system.dict.dto.DictItemUpdateDTO;
import me.north30.erp.system.dict.service.SysDictItemService;
import me.north30.erp.system.dict.vo.DictItemVO;
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
 * 数据字典项管理接口（/api/system/dict-items，接口文档 5.5.5-5.5.8，37-40 号接口）。
 */
@Tag(name = "数据字典项", description = "/api/system/dict-items，接口文档 5.5.5-5.5.8")
@RestController
@RequestMapping("/api/system/dict-items")
@RequiredArgsConstructor
public class SysDictItemController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18031, "message": "字典项已存在", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysDictItemService sysDictItemService;

    /**
     * 5.5.5 字典项查询（按字典类型过滤）。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:dict:list')")
    @Operation(summary = "字典项查询", description = "权限点 system:dict:list；字典类型编码 dictType 必填（缺失返回 10001 参数错误）")
    public Result<PageResult<DictItemVO>> page(@ParameterObject @Valid DictItemQueryDTO query) {
        return Result.success(sysDictItemService.page(query));
    }

    /**
     * 5.5.6 新增字典项。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dict:create')")
    @Operation(summary = "新增字典项", description = "权限点 system:dict:create")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18028 字典类型不存在、18031 字典项已存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> create(@Valid @RequestBody DictItemCreateDTO dto) {
        return Result.success(sysDictItemService.create(dto));
    }

    /**
     * 5.5.7 修改字典项。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:update')")
    @Operation(summary = "修改字典项", description = "权限点 system:dict:update")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10004 字典项不存在、10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> update(@PathVariable Long id, @Valid @RequestBody DictItemUpdateDTO dto) {
        return Result.success(sysDictItemService.update(id, dto));
    }

    /**
     * 5.5.8 删除字典项。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @Operation(summary = "删除字典项", description = "权限点 system:dict:delete")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10004 字典项不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<MutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDictItemService.delete(id));
    }
}
