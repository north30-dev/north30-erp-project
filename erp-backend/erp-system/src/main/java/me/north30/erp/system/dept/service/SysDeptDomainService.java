package me.north30.erp.system.dept.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import me.north30.erp.system.dept.entity.SysDept;

/**
 * 组织/部门服务接口。
 */
public interface SysDeptDomainService {

    /**
     * 按主键查询。
     *
     * @param id 组织部门 ID
     * @return 组织部门实体
     */
    SysDept getById(Long id);

    /**
     * 按组织 ID 集合批量加载组织名称映射（单次 IN 查询，装配用）。
     * <p>返回可变 HashMap：调用方可能以 dept_id=null 做映射取值，不可变 Map.get(null) 会抛 NPE。</p>
     *
     * @param deptIds 组织 ID 集合
     * @return 组织 ID 映射组织名称的映射
     */
    Map<Long, String> getDeptNameMap(Collection<Long> deptIds);

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

    /**
     * 查询指定父级下的同级组织（dept_sort ASC, id ASC，move 规范化重排用）。
     *
     * @param parentId 父级组织 ID（顶级传 null 或 0）
     * @return 同级组织列表
     */
    List<SysDept> listSiblings(Long parentId);

    /**
     * 计算同级下一个显示顺序（当前同级 max(deptSort) + 10，空同级返回 10）。
     * <p>新增组织未显式指定 deptSort 时调用（08 文档阶段四排序统一）。</p>
     *
     * @param parentId 父级组织 ID（顶级传 null 或 0）
     * @return 下一个显示顺序值
     */
    int nextDeptSort(Long parentId);
}
