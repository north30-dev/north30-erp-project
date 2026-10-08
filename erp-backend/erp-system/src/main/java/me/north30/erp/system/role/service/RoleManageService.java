package me.north30.erp.system.role.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.role.dto.RoleAssignMenuDTO;
import me.north30.erp.system.role.dto.RoleCreateDTO;
import me.north30.erp.system.role.dto.RoleDataScopeSaveDTO;
import me.north30.erp.system.role.dto.RolePageQueryDTO;
import me.north30.erp.system.role.dto.RoleUpdateDTO;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.role.vo.RoleCreatedVO;
import me.north30.erp.system.role.vo.RoleDataScopeVO;
import me.north30.erp.system.role.vo.RoleMenuAssignedVO;
import me.north30.erp.system.role.vo.RoleUpdatedVO;
import me.north30.erp.system.role.vo.RoleVO;

/**
 * 角色管理服务接口（接口文档 5.2，7 个接口）。
 */
public interface RoleManageService {

    /**
     * 角色分页查询（含派生 userCount）。
     * @param query 角色分页查询 DTO
     * @return 角色分页查询 VO
     */
    PageResult<RoleVO> page(RolePageQueryDTO query);

    /**
     * 新增角色（role_code 唯一）。
     * @param dto 角色创建 DTO
     * @return 角色创建 VO
     */
    RoleCreatedVO create(RoleCreateDTO dto);

    /**
     * 修改角色（乐观锁，role_code 不可修改）。
     * @param id 角色 ID
     * @param dto 角色更新 DTO
     * @return 角色更新 VO
     */
    RoleUpdatedVO update(Long id, RoleUpdateDTO dto);

    /**
     * 删除角色（逻辑删除；内置角色、已分配用户角色不可删）。
     * @param id 角色 ID
     * @return 删除结果 VO
     */
    DeleteResultVO delete(Long id);

    /**
     * 分配角色菜单权限（全删全插 sys_role_menu）。
     * @param id 角色 ID
     * @param dto 角色菜单分配 DTO
     * @return 角色菜单分配 VO
     */
    RoleMenuAssignedVO assignMenus(Long id, RoleAssignMenuDTO dto);

    /**
     * 配置角色数据范围（全删全插 sys_role_data_scope）。
     * @param id 角色 ID
     * @param dto 角色数据范围配置 DTO
     * @return 角色数据范围配置 VO
     */
    RoleDataScopeVO saveDataScopes(Long id, RoleDataScopeSaveDTO dto);

    /**
     * 查询角色数据范围。
     * @param id 角色 ID
     * @return 角色数据范围 VO
     */
    RoleDataScopeVO listDataScopes(Long id);
}
