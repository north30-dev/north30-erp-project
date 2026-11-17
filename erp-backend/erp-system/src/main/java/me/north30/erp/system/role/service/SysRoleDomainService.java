package me.north30.erp.system.role.service;

import me.north30.erp.system.role.entity.SysRole;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 角色服务接口。
 */
public interface SysRoleDomainService {

    /**
     * 按主键查询角色。
     * @param id 角色 ID
     * @return 角色实体
     */
    SysRole getById(Long id);

    /**
     * 按角色编码查询（逻辑删除过滤）。 
     * @param roleCode 角色编码
     * @return 角色实体
     */
    SysRole getByRoleCode(String roleCode);

    /**
     * 按主键集合批量查询角色。
     * @param ids 角色 ID 集合
     * @return 角色实体列表
     */
    List<SysRole> listByIds(Collection<Long> ids);

    /**
     * 按角色 ID 集合批量加载角色名称映射（单次 IN 查询，装配用）。
     * <p>返回可变 HashMap：调用方可能以 null key 做映射取值，不可变 Map.get(null) 会抛 NPE。</p>
     * @param roleIds 角色 ID 集合
     * @return 角色 ID 映射角色名称的映射
     */
    Map<Long, String> getRoleNameMap(Collection<Long> roleIds);

    /**
     * 新增角色。
     * @param role 角色实体
     * @return 是否新增成功
     */
    boolean createRole(SysRole role);

    /**
     * 校验角色存在，不存在抛 18018（@TableLogic 自动过滤已删除行）。
     * @param id 角色 ID
     * @return 角色实体
     */
    SysRole requireRole(Long id);

    /**
     * 校验角色编码可用，已存在抛 18015（@TableLogic 自动过滤已删除行）。
     * @param roleCode 角色编码
     */
    void requireRoleCodeAvailable(String roleCode);
}
