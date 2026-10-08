package me.north30.erp.system.dept.service;

import me.north30.erp.system.dept.entity.SysDept;

/**
 * 组织/部门服务接口。
 */
public interface SysDeptService {

    /**
     * 按主键查询。
     * 
     * @param id 组织部门 ID
     * @return 组织部门实体
     */
    SysDept getById(Long id);

    /**
     * 校验组织存在，不存在抛 18022（@TableLogic 自动过滤已删除行）。  
     * 
     * @param id 组织 ID
     * @return 组织实体
     */
    SysDept requireDept(Long id);
}
