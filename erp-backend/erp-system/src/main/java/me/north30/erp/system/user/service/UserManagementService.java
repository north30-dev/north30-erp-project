package me.north30.erp.system.user.service;

import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
import me.north30.erp.system.user.vo.UserDeleteVO;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserResetPasswordVO;
import me.north30.erp.system.user.vo.UserStatusVO;
import me.north30.erp.system.user.vo.UserVO;

/**
 * 用户管理服务（接口文档 5.1 用户管理 9 个用例，写操作统一 @AuditLog 审计）。
 */
public interface UserManagementService {

    /**
     * 用户分页查询（5.1.1）。
     */
    PageResult<UserVO> page(UserQueryDTO query);

    /**
     * 用户详情（5.1.2）。
     */
    UserDetailVO getDetail(Long id);

    /**
     * 新增用户（5.1.3），返回新用户 ID。
     */
    Long create(UserCreateDTO dto);

    /**
     * 修改用户（5.1.4），返回更新时间。
     */
    String update(Long id, UserUpdateDTO dto);

    /**
     * 删除用户（5.1.5，逻辑删除）。
     */
    UserDeleteVO delete(Long id);

    /**
     * 启用/停用用户（5.1.6，停用时使该用户全部在线会话失效）。
     */
    UserStatusVO changeStatus(Long id, UserStatusDTO dto);

    /**
     * 重置用户密码（5.1.7）。
     */
    UserResetPasswordVO resetPassword(Long id, UserResetPasswordDTO dto);

    /**
     * 分配用户角色（5.1.8，全量覆盖语义）。
     */
    UserAssignRolesVO assignRoles(Long id, UserAssignRolesDTO dto);

    /**
     * 用户列表导出（5.1.9 简化口径：CSV，UTF-8 BOM，列与列表页一致）。
     */
    byte[] exportCsv(UserQueryDTO query);
}
