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
     * 校验组织存在，不存在抛 18024（@TableLogic 自动过滤已删除行）。
     *
     * @param id 组织 ID
     * @return 组织实体
     */
    SysDept requireDept(Long id);

    /**
     * 校验组织编码是否可用，已存在抛 18026。
     *
     * @param deptCode 组织编码
     */
    void requireDeptCodeAvailable(String deptCode);

    /**
     * 校验组织无下级组织，有则抛 18025（删除前置守卫）。
     *
     * @param dept 组织实体
     */
    void requireDeptHasNoChildren(SysDept dept);

    /**
     * 校验组织无绑定用户，有则抛 18025（删除前置守卫）。
     *
     * @param dept 组织实体
     */
    void requireDeptHasNoUsers(SysDept dept);
}
