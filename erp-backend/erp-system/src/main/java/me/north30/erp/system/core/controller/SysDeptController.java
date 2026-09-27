package me.north30.erp.system.core.controller;

import jakarta.validation.Valid;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.core.dto.DeptCreateDTO;
import me.north30.erp.system.core.dto.DeptTreeQueryDTO;
import me.north30.erp.system.core.dto.DeptUpdateDTO;
import me.north30.erp.system.core.service.SysDeptManageService;
import me.north30.erp.system.core.vo.DeptMutationVO;
import me.north30.erp.system.core.vo.DeptTreeVO;
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
 * 组织与部门管理接口（/api/system/depts，接口文档 5.4，29-32 号接口）。
 */
@RestController
@RequestMapping("/api/system/depts")
public class SysDeptController {

    private final SysDeptManageService sysDeptManageService;

    public SysDeptController(SysDeptManageService sysDeptManageService) {
        this.sysDeptManageService = sysDeptManageService;
    }

    /**
     * 5.4.1 组织部门树查询。
     */
    @GetMapping("/tree")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public Result<List<DeptTreeVO>> tree(DeptTreeQueryDTO query) {
        return Result.success(sysDeptManageService.listTree(query));
    }

    /**
     * 5.4.2 新增组织部门。
     */
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:create')")
    public Result<DeptMutationVO> create(@Valid @RequestBody DeptCreateDTO dto) {
        return Result.success(sysDeptManageService.create(dto));
    }

    /**
     * 5.4.3 修改组织部门。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:update')")
    public Result<DeptMutationVO> update(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        return Result.success(sysDeptManageService.update(id, dto));
    }

    /**
     * 5.4.4 删除组织部门。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    public Result<DeptMutationVO> delete(@PathVariable Long id) {
        return Result.success(sysDeptManageService.delete(id));
    }
}
