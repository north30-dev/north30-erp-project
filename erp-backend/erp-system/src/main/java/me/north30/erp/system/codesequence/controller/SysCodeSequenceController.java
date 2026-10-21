package me.north30.erp.system.codesequence.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.codesequence.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.codesequence.dto.CodeSequenceResetDTO;
import me.north30.erp.system.codesequence.service.SysCodeSequenceService;
import me.north30.erp.system.codesequence.vo.CodeSequenceResetVO;
import me.north30.erp.system.codesequence.vo.CodeSequenceVO;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单据编号序列管理接口（/api/system/code-sequences，接口文档 5.7，45-46 号接口）。
 */
@Tag(name = "单据编号序列", description = "/api/system/code-sequences，接口文档 5.7")
@RestController
@RequestMapping("/api/system/code-sequences")
@RequiredArgsConstructor
public class SysCodeSequenceController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18036, "message": "流水号重置值不得小于当前已用流水号", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final SysCodeSequenceService sysCodeSequenceService;

    /**
     * 5.7.1 编号序列分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:sequence:list')")
    @Operation(summary = "编号序列分页查询", description = "权限点 system:sequence:list")
    public Result<PageResult<CodeSequenceVO>> page(@ParameterObject @Valid CodeSequenceQueryDTO query) {
        return Result.success(sysCodeSequenceService.page(query));
    }

    /**
     * 5.7.2 重置编号流水。
     */
    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('system:sequence:reset')")
    @Operation(summary = "重置编号流水", description = "权限点 system:sequence:reset；仅允许向下重置至未使用区间，目标值相同时幂等跳过")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18035 编号序列不存在、18036 重置值小于当前已用流水号、10601 乐观锁冲突）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<CodeSequenceResetVO> reset(@Valid @RequestBody CodeSequenceResetDTO dto) {
        return Result.success(sysCodeSequenceService.reset(dto));
    }
}
