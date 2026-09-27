package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.CodeSequenceQueryDTO;
import me.north30.erp.system.core.dto.CodeSequenceResetDTO;
import me.north30.erp.system.core.service.ISysCodeSequenceService;
import me.north30.erp.system.core.vo.CodeSequenceResetVO;
import me.north30.erp.system.core.vo.CodeSequenceVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 单据编号序列管理接口（/api/system/code-sequences，接口文档 5.7，45-46 号接口）。
 */
@RestController
@RequestMapping("/api/system/code-sequences")
public class SysCodeSequenceController {

    private final ISysCodeSequenceService sysCodeSequenceService;

    public SysCodeSequenceController(ISysCodeSequenceService sysCodeSequenceService) {
        this.sysCodeSequenceService = sysCodeSequenceService;
    }

    /**
     * 5.7.1 编号序列分页查询。
     */
    @GetMapping
    @PreAuthorize("hasAuthority('system:sequence:list')")
    public Result<PageResult<CodeSequenceVO>> page(CodeSequenceQueryDTO query) {
        return Result.success(sysCodeSequenceService.page(query));
    }

    /**
     * 5.7.2 重置编号流水。
     */
    @PostMapping("/reset")
    @PreAuthorize("hasAuthority('system:sequence:reset')")
    public Result<CodeSequenceResetVO> reset(@Valid @RequestBody CodeSequenceResetDTO dto) {
        return Result.success(sysCodeSequenceService.reset(dto));
    }
}
