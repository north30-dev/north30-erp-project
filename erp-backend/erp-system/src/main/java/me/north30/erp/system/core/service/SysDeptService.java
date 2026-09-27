package me.north30.erp.system.core.service;

import me.north30.erp.system.core.entity.SysDept;

/**
 * 组织/部门服务接口。
 */
public interface SysDeptService {

    /**
     * 按主键查询。
     */
    SysDept getById(Long id);
}
