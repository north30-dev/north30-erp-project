package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.WarehouseIdCodecUtil;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.enums.UserErrorCode;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.user.converter.UserConverter;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.user.service.SysUserService;
import me.north30.erp.system.user.service.UserManagementService;
import me.north30.erp.system.user.strategy.PasswordStrategy;
import me.north30.erp.system.user.strategy.UserAssembleStrategy;
import me.north30.erp.system.user.strategy.UserQueryStrategy;
import me.north30.erp.system.user.strategy.UserRoleStrategy;
import me.north30.erp.system.user.strategy.UserSessionRevokeStrategy;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
import me.north30.erp.system.user.vo.UserDeleteVO;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserResetPasswordVO;
import me.north30.erp.system.user.vo.UserStatusVO;
import me.north30.erp.system.user.vo.UserVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 用户管理服务实现（纯编排，零私有方法）。
 * <p>职责分工：状态守卫进 {@link SysUser}（充血方法），纯规则与取数装配/跨域关联/会话清理
 * 进 user.strategy 包各策略类，本类只负责"查库 → 调实体/策略 → 持久化 → 组装响应"的流程编排。
 * 聚合 sys_user/sys_user_role/sys_role/sys_dept 四表操作均在 erp-system 模块内，
 * 策略类直接复用同模块 Mapper 以免为既有 Service 接口扩权（跨模块仍禁止直接调用 Mapper）。</p>
 * <p>写操作统一显式事务 + @AuditLog 审计；密码 BCrypt 加密且不回显、不入审计 JSON（S-06）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    /** 权限变更生效提示（接口文档 5.1.8 固定文案） */
    private static final String PERMISSION_REFRESH_TIP = "权限变更将在 5 分钟内或重新登录后生效";

    private final SysUserMapper sysUserMapper;
    private final SysDeptMapper sysDeptMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final SysUserService sysUserService;
    private final UserQueryStrategy userQueryStrategy;
    private final UserAssembleStrategy userAssembleStrategy;
    private final UserRoleStrategy userRoleStrategy;
    private final UserSessionRevokeStrategy userSessionRevokeStrategy;

    /**
     * 分页查询用户列表。
     *
     * @param query 查询参数
     * @return PageResult<UserVO> 用户列表分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<UserVO> page(UserQueryDTO query) {
        Page<SysUser> page = sysUserMapper.selectPage(
            new Page<>(userQueryStrategy.resolvePageNum(query), userQueryStrategy.resolvePageSize(query)),
            userQueryStrategy.buildListWrapper(query).orderByDesc(SysUser::getCreateTime));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
            userAssembleStrategy.toUserVOs(page.getRecords()));
    }

    /**
     * 查询用户详情。
     *
     * @param id 用户 ID
     * @return UserDetailVO 用户详情
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetailVO getDetail(Long id) {
        SysUser user = sysUserService.requireUser(id);
        List<Long> roleIds = userRoleStrategy.listRoleIdsByUserId(id);
        Map<Long, String> roleCodeMap = userAssembleStrategy.loadRoleCodeMap(roleIds);
        List<String> roles = roleIds.stream().map(roleCodeMap::get).filter(Objects::nonNull).toList();
        SysDept dept = user.getDeptId() != null ? sysDeptMapper.selectById(user.getDeptId()) : null;
        return userConverter.toDetailVO(user, dept, roles, roleIds,
            WarehouseIdCodecUtil.parse(user.getWarehouseIds()));
    }

    /**
     * 创建用户。
     *
     * @param dto 创建参数
     * @return Long 创建的用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    public Long create(UserCreateDTO dto) {
        // 1. username/user_code 全局唯一校验（接口文档 5.1.3：18006/18007）
        sysUserService.requireUsernameAvailable(dto.username());
        sysUserService.requireUserCodeAvailable(dto.userCode());
        // 2. 组织存在性校验（18024）；角色集合去重去空后校验存在性（18014）
        if (dto.deptId() != null && sysDeptMapper.selectById(dto.deptId()) == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + dto.deptId() + " 不存在");
        }
        List<Long> roleIds = dto.roleIds() == null ? List.of()
            : dto.roleIds().stream().filter(Objects::nonNull).distinct().toList();
        userRoleStrategy.requireAllExist(roleIds);
        // 3. 落库：口令复杂度校验（18009）+ BCrypt 加密 + 初始口令修改时间置为当前（90 天有效期基准）
        SysUser user = userConverter.toEntity(dto);
        user.initPassword(dto.password(), passwordEncoder, LocalDateTime.now());
        user.setWarehouseIds(WarehouseIdCodecUtil.join(dto.warehouseIds()));
        if (user.getGender() == null) {
            user.setGender(0);
        }
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        user.setIsAdmin(0);
        sysUserMapper.insert(user);
        userRoleStrategy.insertForUser(user.getId(), roleIds);
        return user.getId();
    }

    /**
     * 更新用户。
     *
     * @param id 用户 ID
     * @param dto 更新参数
     * @return String 更新时间
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public String update(Long id, UserUpdateDTO dto) {
        SysUser user = sysUserService.requireUser(id);
        // 停用内置管理员视为受保护操作（18012）；update 语义是资料更新，不走 changeStatus 实体方法
        if (user.isAdmin() && dto.status() != null && dto.status() == 0) {
            throw new BusinessException(UserErrorCode.ADMIN_PROTECTED);
        }
        if (dto.deptId() != null && sysDeptMapper.selectById(dto.deptId()) == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + dto.deptId() + " 不存在");
        }
        SysUser entity = userConverter.toEntity(dto);
        entity.setId(id);
        entity.setWarehouseIds(dto.warehouseIds() != null ? WarehouseIdCodecUtil.join(dto.warehouseIds()) : null);
        // 带乐观锁版本条件更新，冲突时更新行数为 0；显式设置更新时间（strictUpdateFill 不覆盖已设值）
        entity.setUpdateTime(LocalDateTime.now());
        if (sysUserMapper.updateById(entity) <= 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        return DateTimeFormatUtil.format(entity.getUpdateTime());
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return UserDeleteVO 删除响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    public UserDeleteVO delete(Long id) {
        SysUser user = sysUserService.requireUser(id);
        // 内置管理员不可删除（18012）
        if (user.isAdmin()) {
            throw new BusinessException(UserErrorCode.ADMIN_PROTECTED);
        }
        // 存在角色分配即视为被引用，拒绝删除（18013）
        if (userRoleStrategy.countByUserId(id) > 0) {
            throw new BusinessException(UserErrorCode.USER_REFERENCED,
                "用户 " + user.getUsername() + " 已分配角色，不可删除（请先解除角色分配或选择停用）");
        }
        // @TableLogic 逻辑删除，禁止物理 DELETE
        sysUserMapper.deleteById(id);
        return new UserDeleteVO(id, 1);
    }

    /**
     * 更新用户状态。
     *
     * @param id 用户 ID
     * @param dto 状态更新参数
     * @return UserStatusVO 状态更新响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserStatusVO changeStatus(Long id, UserStatusDTO dto) {
        SysUser user = sysUserService.requireUser(id);
        // 非法状态值 → 10001（注解校验兜底，服务层再守卫一次）
        if (dto.status() == null || (dto.status() != 0 && dto.status() != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态取值非法，仅支持 0-停用 1-启用");
        }
        // 内置管理员不可停用（18012），守卫收敛于实体 changeStatus
        user.changeStatus(dto.status());
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setStatus(user.getStatus());
        patch.setVersion(dto.version());
        if (sysUserMapper.updateById(patch) <= 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        boolean sessionRevoked = false;
        if (dto.status() == 0) {
            // 停用后该用户全部在线会话立即失效（删除 system:session:{userId}:* 与 refresh 标记；
            // Redis 不可用时策略内降级 WARN，仍视为已触发失效）
            userSessionRevokeStrategy.revoke(id);
            sessionRevoked = true;
        }
        return new UserStatusVO(dto.status(), sessionRevoked);
    }

    /**
     * 重置用户密码。
     *
     * @param id 用户 ID
     * @param dto 重置密码参数
     * @return UserResetPasswordVO 重置密码响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserResetPasswordVO resetPassword(Long id, UserResetPasswordDTO dto) {
        sysUserService.requireUser(id);
        // 缺省时服务端生成随机口令；指定口令则校验复杂度（18009）
        String initialPassword = StringUtils.hasText(dto.newPassword()) ? dto.newPassword() : PasswordStrategy.generate();
        PasswordStrategy.checkOrThrow(initialPassword);
        // password_update_time 置空触发下次登录强制改密（接口文档 5.1.7 forceChangeOnLogin=true 口径）；
        // 通过空实体承载 update_by/update_time 自动填充，wrapper.set 显式将口令修改时间置 NULL
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, id)
            .set(SysUser::getPassword, passwordEncoder.encode(initialPassword))
            .set(SysUser::getPasswordUpdateTime, null);
        sysUserMapper.update(new SysUser(), wrapper);
        return new UserResetPasswordVO(initialPassword, Boolean.TRUE);
    }

    /**
     * 为用户分配角色。
     *
     * @param id 用户 ID
     * @param dto 角色分配参数
     * @return UserAssignRolesVO 角色分配响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserAssignRolesVO assignRoles(Long id, UserAssignRolesDTO dto) {
        sysUserService.requireUser(id);
        List<Long> roleIds = dto.roleIds() == null ? List.of()
            : dto.roleIds().stream().filter(Objects::nonNull).distinct().toList();
        userRoleStrategy.requireAllExist(roleIds);
        userRoleStrategy.replaceAllForUser(id, roleIds);
        return new UserAssignRolesVO(roleIds, PERMISSION_REFRESH_TIP);
    }

    /**
     * 导出用户列表为 CSV 文件。
     *
     * @param query 查询参数
     * @return byte[] CSV 文件内容
     */
    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(UserQueryDTO query) {
        List<SysUser> users = sysUserMapper.selectList(
            userQueryStrategy.buildListWrapper(query).orderByDesc(SysUser::getCreateTime));
        return userAssembleStrategy.toCsvBytes(users);
    }
}
