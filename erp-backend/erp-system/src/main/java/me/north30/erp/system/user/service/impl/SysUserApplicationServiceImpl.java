package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.util.UpdateGuardUtil;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.common.util.DateTimeFormatUtil;
import me.north30.erp.common.util.WarehouseIdCodecUtil;
import me.north30.erp.system.common.enums.SystemErrorCode;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.role.service.SysRoleDomainService;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.security.SecurityUtils;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.common.vo.DeleteResultVO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.dept.service.SysDeptDomainService;
import me.north30.erp.system.user.converter.UserConverter;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.user.service.SysUserDomainService;
import me.north30.erp.system.user.service.SysUserApplicationService;
import me.north30.erp.system.user.strategy.PasswordStrategy;
import me.north30.erp.system.user.strategy.UserAssembleStrategy;
import me.north30.erp.system.user.strategy.UserQueryStrategy;
import me.north30.erp.system.user.strategy.UserRoleStrategy;
import me.north30.erp.system.user.strategy.UserSessionRevokeStrategy;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
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
 * <p>写操作统一显式事务；审计日志由 Controller 层 @AuditLog 记录（见编码规范）；密码 BCrypt 加密且不回显、不入审计 JSON（S-06）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserApplicationServiceImpl implements SysUserApplicationService {

    /** 权限变更生效提示（接口文档 5.1.8 固定文案） */
    private static final String PERMISSION_REFRESH_TIP = "权限变更将在 5 分钟内或重新登录后生效";

    private final SysUserMapper sysUserMapper;
    private final SysDeptDomainService sysDeptDomainService;
    private final SysRoleDomainService sysRoleDomainService;
    private final UserAccessService userAccessService;
    private final PasswordEncoder passwordEncoder;
    private final UserConverter userConverter;
    private final SysUserDomainService sysUserDomainService;
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
        SysUser user = sysUserDomainService.requireUser(id);
        List<Long> roleIds = userRoleStrategy.listRoleIdsByUserId(id);
        Map<Long, String> roleNameMap = userAssembleStrategy.loadRoleNameMap(roleIds);
        List<String> roles = roleIds.stream().map(roleNameMap::get).filter(Objects::nonNull).toList();
        SysDept dept = user.getDeptId() != null ? sysDeptDomainService.getById(user.getDeptId()) : null;
        return userConverter.toUserDetailVO(user, dept, roles, roleIds,
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
    public Long create(UserCreateDTO dto) {
        // 1. username/user_code 全局唯一校验（接口文档 5.1.3：18006/18007）
        sysUserDomainService.requireUsernameAvailable(dto.username());
        sysUserDomainService.requireUserCodeAvailable(dto.userCode());
        // 2. 组织存在性校验（18024）；角色集合去重去空后校验存在性（18014）
        if (dto.deptId() != null) {
            sysDeptDomainService.requireDept(dto.deptId());
        }
        List<Long> roleIds = dto.roleIds() == null ? List.of()
            : dto.roleIds().stream().filter(Objects::nonNull).distinct().toList();
        userRoleStrategy.requireAllExist(roleIds);
        requireAssignAdminAllowed(roleIds);
        // 3. 落库：口令复杂度校验（18009）+ BCrypt 加密 + 初始口令修改时间置为当前（90 天有效期基准）
        SysUser user = userConverter.createUser(dto);
        user.initPassword(dto.password(), passwordEncoder, LocalDateTime.now());
        user.setWarehouseIds(WarehouseIdCodecUtil.join(dto.warehouseIds()));
        
        user.applyCreateDefault();
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
    public String update(Long id, UserUpdateDTO dto) {
        SysUser user = sysUserDomainService.requireUser(id);
        // update 语义是资料更新，状态变更统一走 changeStatus 专用接口（内置管理员保护在 changeStatus 中校验）
        if (dto.deptId() != null) {
            sysDeptDomainService.requireDept(dto.deptId());
        }
        SysUser entity = userConverter.updateUser(dto);
        entity.setId(id);
        entity.setWarehouseIds(dto.warehouseIds() != null ? WarehouseIdCodecUtil.join(dto.warehouseIds()) : null);
        // 带乐观锁版本条件更新，冲突时更新行数为 0；显式设置更新时间（strictUpdateFill 不覆盖已设值）
        entity.setUpdateTime(LocalDateTime.now());
        UpdateGuardUtil.checkOptimisticLockConflict(sysUserMapper.updateById(entity));
        return DateTimeFormatUtil.format(entity.getUpdateTime());
    }

    /**
     * 删除用户。
     *
     * @param id 用户 ID
     * @return DeleteResultVO 删除响应
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DeleteResultVO delete(Long id) {
        SysUser user = sysUserDomainService.requireUser(id);
        // 内置管理员不可删除（18012）
        user.requireNotAdmin();
        // 存在角色分配即视为被引用，拒绝删除（18013）
        if (userRoleStrategy.countByUserId(id) > 0) {
            throw new BusinessException(SystemErrorCode.USER_REFERENCED,
                "用户 " + user.getUsername() + " 已分配角色，不可删除（请先解除角色分配或选择停用）");
        }
        // @TableLogic 逻辑删除，禁止物理 DELETE
        sysUserMapper.deleteById(id);
        return new DeleteResultVO(id, 1);
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
    public UserStatusVO changeStatus(Long id, UserStatusDTO dto) {
        SysUser user = sysUserDomainService.requireUser(id);
        // 状态值域守卫（10001）与内置管理员不可停用（18012）收敛于实体 changeStatus
        user.changeStatus(dto.status());
        SysUser patch = new SysUser();
        patch.setId(id);
        patch.setStatus(user.getStatus());
        patch.setVersion(dto.version());
        UpdateGuardUtil.checkOptimisticLockConflict(sysUserMapper.updateById(patch));
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
    public UserResetPasswordVO resetPassword(Long id, UserResetPasswordDTO dto) {
        sysUserDomainService.requireUser(id);
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
    public UserAssignRolesVO assignRoles(Long id, UserAssignRolesDTO dto) {
        sysUserDomainService.requireUser(id);
        List<Long> roleIds = dto.roleIds() == null ? List.of()
            : dto.roleIds().stream().filter(Objects::nonNull).distinct().toList();
        userRoleStrategy.requireAllExist(roleIds);
        requireAssignAdminAllowed(roleIds);
        userRoleStrategy.replaceAllForUser(id, roleIds);
        return new UserAssignRolesVO(roleIds, PERMISSION_REFRESH_TIP);
    }

    /**
     * 提权守卫（18040）：目标角色含 ADMIN 时要求操作者为超管（08 文档 2.1），
     * 防止任何可操作角色分配者自我提权。create 与 assignRoles 共用。
     *
     * @param roleIds 本次待分配的角色 ID 集合
     */
    private void requireAssignAdminAllowed(List<Long> roleIds) {
        boolean containsAdmin = !roleIds.isEmpty()
            && sysRoleDomainService.listByIds(roleIds).stream()
                .anyMatch(role -> SysRole.ROLE_CODE_ADMIN.equals(role.getRoleCode()));
        if (!containsAdmin) {
            return;
        }
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        if (!userAccessService.isSuperAdmin(currentUser.userId())) {
            throw new BusinessException(SystemErrorCode.ONLY_SUPER_ADMIN_ASSIGN_ADMIN);
        }
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
