package me.north30.erp.system.dept.service;

import me.north30.erp.system.dept.entity.SysDept;

/**
 * 组织/部门服务接口。
 */
public interface SysDeptService {

    /**
     * 按主键查询。
     */
    SysDept getById(Long id);
}
