package me.north30.erp.system.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.audit.AuditLog;
import me.north30.erp.common.audit.AuditModuleEnum;
import me.north30.erp.common.audit.OperateTypeEnum;
import me.north30.erp.common.constant.PageConstants;
import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.exception.SystemErrorCode;
import me.north30.erp.common.result.PageResult;
import me.north30.erp.system.user.dto.UserAssignRolesDTO;
import me.north30.erp.system.user.dto.UserCreateDTO;
import me.north30.erp.system.user.dto.UserQueryDTO;
import me.north30.erp.system.user.dto.UserResetPasswordDTO;
import me.north30.erp.system.user.dto.UserStatusDTO;
import me.north30.erp.system.user.dto.UserUpdateDTO;
import me.north30.erp.system.dept.entity.SysDept;
import me.north30.erp.system.role.entity.SysRole;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.role.entity.SysUserRole;
import me.north30.erp.system.common.enums.RoleMenuErrorCode;
import me.north30.erp.system.common.enums.SystemManageErrorCode;
import me.north30.erp.system.common.enums.UserErrorCode;
import me.north30.erp.system.dept.mapper.SysDeptMapper;
import me.north30.erp.system.role.mapper.SysRoleMapper;
import me.north30.erp.system.user.mapper.SysUserMapper;
import me.north30.erp.system.role.mapper.SysUserRoleMapper;
import me.north30.erp.system.user.service.UserManagementService;
import me.north30.erp.system.user.vo.UserAssignRolesVO;
import me.north30.erp.system.user.vo.UserDeleteVO;
import me.north30.erp.system.user.vo.UserDetailVO;
import me.north30.erp.system.user.vo.UserResetPasswordVO;
import me.north30.erp.system.user.vo.UserStatusVO;
import me.north30.erp.system.user.vo.UserVO;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现。
 * <p>本服务聚合 sys_user/sys_user_role/sys_role/sys_dept 四表操作，均在 erp-system 模块内，
 * 直接复用同模块 Mapper 以免为既有 Service 接口扩权（跨模块仍禁止直接调用 Mapper）。</p>
 * <p>写操作统一显式事务 + @AuditLog 审计；密码 BCrypt 加密且不回显、不入审计 JSON（S-06）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManagementServiceImpl implements UserManagementService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** CSV 导出 UTF-8 BOM（Excel 兼容） */
    private static final String CSV_BOM = "\uFEFF";

    /** 权限变更生效提示（接口文档 5.1.8 固定文案） */
    private static final String PERMISSION_REFRESH_TIP = "权限变更将在 5 分钟内或重新登录后生效";

    /** 随机初始口令长度（须满足复杂度：4 类字符各至少 1 个） */
    private static final int RANDOM_PASSWORD_LENGTH = 12;

    private static final String PASSWORD_UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    private static final String PASSWORD_LOWER = "abcdefghijklmnopqrstuvwxyz";

    private static final String PASSWORD_DIGIT = "0123456789";

    private static final String PASSWORD_SPECIAL = "!@#$%^&*";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysDeptMapper sysDeptMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResult<UserVO> page(UserQueryDTO query) {
        int pageNum = resolvePageNum(query);
        int pageSize = resolvePageSize(query);
        Page<SysUser> page = sysUserMapper.selectPage(new Page<>(pageNum, pageSize),
            buildQueryWrapper(query).orderByDesc(SysUser::getCreateTime));
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), toUserVOs(page.getRecords()));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetailVO getDetail(Long id) {
        SysUser user = requireUser(id);
        List<Long> roleIds = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id)).stream()
            .map(SysUserRole::getRoleId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
        Map<Long, String> roleCodeMap = loadRoleCodeMap(roleIds);
        List<String> roles = roleIds.stream().map(roleCodeMap::get).filter(Objects::nonNull).toList();
        SysDept dept = user.getDeptId() != null ? sysDeptMapper.selectById(user.getDeptId()) : null;
        return new UserDetailVO(
            user.getId(), user.getUserCode(), user.getUsername(), user.getRealName(),
            maskPhone(user.getPhone()), user.getEmail(), user.getDeptId(),
            dept != null ? dept.getDeptName() : null,
            roles, roleIds, parseWarehouseIds(user.getWarehouseIds()),
            user.getStatus(), user.getIsAdmin(), user.getGender(), user.getRemark(),
            user.getLoginFailCount(),
            formatTime(user.getLockUntil()), formatTime(user.getPasswordUpdateTime()),
            formatTime(user.getLastLoginTime()), formatTime(user.getCreateTime()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.CREATE)
    public Long create(UserCreateDTO dto) {
        // 1. username/user_code 全局唯一校验（接口文档 5.1.3：18006/18007）
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.username())) > 0) {
            throw new BusinessException(UserErrorCode.USERNAME_EXISTS, "用户名 " + dto.username() + " 已存在");
        }
        if (sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserCode, dto.userCode())) > 0) {
            throw new BusinessException(UserErrorCode.USER_CODE_EXISTS, "用户编号 " + dto.userCode() + " 已存在");
        }
        // 2. 初始口令复杂度（18009，规则同 4.8）
        if (!matchesComplexity(dto.password())) {
            throw new BusinessException(SystemErrorCode.PASSWORD_COMPLEXITY_ERROR);
        }
        // 3. 组织与角色存在性校验（18024/18014）
        if (dto.deptId() != null) {
            requireDeptExists(dto.deptId());
        }
        List<Long> roleIds = distinctRoleIds(dto.roleIds());
        validateRolesExist(roleIds);
        // 4. 落库：口令 BCrypt 加密，初始口令修改时间置为当前（90 天有效期基准）
        SysUser user = new SysUser();
        user.setUserCode(dto.userCode());
        user.setUsername(dto.username());
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.setRealName(dto.realName());
        user.setDeptId(dto.deptId());
        user.setWarehouseIds(joinWarehouseIds(dto.warehouseIds()));
        user.setPhone(dto.phone());
        user.setEmail(dto.email());
        user.setGender(dto.gender() != null ? dto.gender() : 0);
        user.setStatus(dto.status() != null ? dto.status() : 1);
        user.setIsAdmin(0);
        user.setPasswordUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);
        insertUserRoles(user.getId(), roleIds);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public String update(Long id, UserUpdateDTO dto) {
        SysUser user = requireUser(id);
        // 停用内置管理员视为受保护操作（18012）
        if (isAdmin(user) && dto.status() != null && dto.status() == 0) {
            throw new BusinessException(UserErrorCode.ADMIN_PROTECTED);
        }
        if (dto.deptId() != null) {
            requireDeptExists(dto.deptId());
        }
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setRealName(dto.realName());
        entity.setDeptId(dto.deptId());
        entity.setWarehouseIds(dto.warehouseIds() != null ? joinWarehouseIds(dto.warehouseIds()) : null);
        entity.setPhone(dto.phone());
        entity.setEmail(dto.email());
        entity.setGender(dto.gender());
        entity.setStatus(dto.status());
        entity.setRemark(dto.remark());
        // 带乐观锁版本条件更新，冲突时更新行数为 0；显式设置更新时间（strictUpdateFill 不覆盖已设值）
        entity.setVersion(dto.version());
        entity.setUpdateTime(LocalDateTime.now());
        if (sysUserMapper.updateById(entity) <= 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        return formatTime(entity.getUpdateTime());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.DELETE)
    public UserDeleteVO delete(Long id) {
        SysUser user = requireUser(id);
        // 内置管理员不可删除（18012）
        if (isAdmin(user)) {
            throw new BusinessException(UserErrorCode.ADMIN_PROTECTED);
        }
        // 存在角色分配即视为被引用，拒绝删除（18013）
        Long roleCount = sysUserRoleMapper.selectCount(
            new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        if (roleCount != null && roleCount > 0) {
            throw new BusinessException(UserErrorCode.USER_REFERENCED,
                "用户 " + user.getUsername() + " 已分配角色，不可删除（请先解除角色分配或选择停用）");
        }
        // @TableLogic 逻辑删除，禁止物理 DELETE
        sysUserMapper.deleteById(id);
        return new UserDeleteVO(id, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserStatusVO changeStatus(Long id, UserStatusDTO dto) {
        SysUser user = requireUser(id);
        // 非法状态值 → 10001（注解校验兜底，服务层再守卫一次）
        if (dto.status() == null || (dto.status() != 0 && dto.status() != 1)) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "状态取值非法，仅支持 0-停用 1-启用");
        }
        // 内置管理员不可停用（18012）
        if (isAdmin(user) && dto.status() == 0) {
            throw new BusinessException(UserErrorCode.ADMIN_PROTECTED);
        }
        SysUser entity = new SysUser();
        entity.setId(id);
        entity.setStatus(dto.status());
        entity.setVersion(dto.version());
        if (sysUserMapper.updateById(entity) <= 0) {
            throw new BusinessException(CommonErrorCode.OPTIMISTIC_LOCK_CONFLICT);
        }
        boolean sessionRevoked = false;
        if (dto.status() == 0) {
            // 停用后该用户全部在线会话立即失效（删除 system:session:{userId}:* 与 refresh 标记）
            revokeUserSessions(id);
            sessionRevoked = true;
        }
        return new UserStatusVO(dto.status(), sessionRevoked);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserResetPasswordVO resetPassword(Long id, UserResetPasswordDTO dto) {
        requireUser(id);
        // 缺省时服务端生成随机口令；指定口令则校验复杂度（18009）
        String initialPassword = StringUtils.hasText(dto.newPassword()) ? dto.newPassword() : generateRandomPassword();
        if (!matchesComplexity(initialPassword)) {
            throw new BusinessException(SystemErrorCode.PASSWORD_COMPLEXITY_ERROR);
        }
        // password_update_time 置空触发下次登录强制改密（接口文档 5.1.7 forceChangeOnLogin=true 口径）；
        // 通过空实体承载 update_by/update_time 自动填充，wrapper.set 显式将口令修改时间置 NULL
        LambdaUpdateWrapper<SysUser> wrapper = new LambdaUpdateWrapper<SysUser>()
            .eq(SysUser::getId, id)
            .set(SysUser::getPassword, passwordEncoder.encode(initialPassword))
            .set(SysUser::getPasswordUpdateTime, null);
        sysUserMapper.update(new SysUser(), wrapper);
        return new UserResetPasswordVO(initialPassword, Boolean.TRUE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(module = AuditModuleEnum.SYSTEM, operateType = OperateTypeEnum.UPDATE)
    public UserAssignRolesVO assignRoles(Long id, UserAssignRolesDTO dto) {
        requireUser(id);
        List<Long> roleIds = distinctRoleIds(dto.roleIds());
        validateRolesExist(roleIds);
        // 全删全插（全量覆盖语义）：sys_user_role 带 @TableLogic，delete 为逻辑删除，
        // 唯一索引 uk_sys_user_role 仅约束 is_deleted=0 行，重插不冲突
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, id));
        insertUserRoles(id, roleIds);
        return new UserAssignRolesVO(roleIds, PERMISSION_REFRESH_TIP);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCsv(UserQueryDTO query) {
        List<SysUser> users = sysUserMapper.selectList(
            buildQueryWrapper(query).orderByDesc(SysUser::getCreateTime));
        Map<Long, String> deptNames = loadDeptNames(users);
        Map<Long, List<String>> roleCodes = loadRoleCodesByUserIds(
            users.stream().map(SysUser::getId).toList());
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_BOM);
        csv.append("用户ID,用户编号,用户名,姓名,联系电话,邮箱,所属组织,角色,状态,是否管理员,最后登录时间,创建时间").append("\r\n");
        for (SysUser user : users) {
            csv.append(escape(String.valueOf(user.getId()))).append(',')
                .append(escape(user.getUserCode())).append(',')
                .append(escape(user.getUsername())).append(',')
                .append(escape(user.getRealName())).append(',')
                .append(escape(maskPhone(user.getPhone()))).append(',')
                .append(escape(user.getEmail())).append(',')
                .append(escape(deptNames.get(user.getDeptId()))).append(',')
                .append(escape(String.join(";", roleCodes.getOrDefault(user.getId(), List.of())))).append(',')
                .append(escape(user.getStatus() != null && user.getStatus() == 1 ? "启用" : "停用")).append(',')
                .append(escape(isAdmin(user) ? "是" : "否")).append(',')
                .append(escape(formatTime(user.getLastLoginTime()))).append(',')
                .append(escape(formatTime(user.getCreateTime())))
                .append("\r\n");
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    /**
     * 构建列表查询条件：username/realName 模糊、userCode 精确、status 精确、deptId 含下级组织。
     */
    private LambdaQueryWrapper<SysUser> buildQueryWrapper(UserQueryDTO query) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
            .like(StringUtils.hasText(query.username()), SysUser::getUsername, query.username())
            .like(StringUtils.hasText(query.realName()), SysUser::getRealName, query.realName())
            .eq(StringUtils.hasText(query.userCode()), SysUser::getUserCode, query.userCode())
            .eq(query.status() != null, SysUser::getStatus, query.status());
        if (query.deptId() != null) {
            wrapper.in(SysUser::getDeptId, resolveDeptIdsWithDescendants(query.deptId()));
        }
        return wrapper;
    }

    /**
     * 解析组织及下级组织 ID 集合：组织规模小（≤百级），一次全量查询后按 ancestors 路径内存过滤，
     * 避免 LIKE 拼接注入面与循环查库。
     */
    private List<Long> resolveDeptIdsWithDescendants(Long deptId) {
        List<SysDept> depts = sysDeptMapper.selectList(
            new LambdaQueryWrapper<SysDept>().select(SysDept::getId, SysDept::getAncestors));
        List<Long> deptIds = new ArrayList<>();
        deptIds.add(deptId);
        for (SysDept dept : depts) {
            if (dept.getId().equals(deptId)) {
                continue;
            }
            if (containsAncestor(dept.getAncestors(), deptId)) {
                deptIds.add(dept.getId());
            }
        }
        return deptIds;
    }

    /**
     * 判断 ancestors 祖级路径（如 0,1,5）是否包含指定组织 ID。
     */
    private boolean containsAncestor(String ancestors, Long deptId) {
        if (ancestors == null || ancestors.isBlank()) {
            return false;
        }
        return Arrays.stream(ancestors.split(","))
            .anyMatch(part -> part.trim().equals(String.valueOf(deptId)));
    }

    private List<UserVO> toUserVOs(List<SysUser> users) {
        if (users.isEmpty()) {
            return List.of();
        }
        Map<Long, String> deptNames = loadDeptNames(users);
        Map<Long, List<String>> roleCodes = loadRoleCodesByUserIds(
            users.stream().map(SysUser::getId).toList());
        return users.stream().map(user -> new UserVO(
            user.getId(), user.getUserCode(), user.getUsername(), user.getRealName(),
            maskPhone(user.getPhone()), user.getEmail(), user.getDeptId(),
            deptNames.get(user.getDeptId()),
            roleCodes.getOrDefault(user.getId(), List.of()),
            user.getStatus(), user.getIsAdmin(),
            formatTime(user.getLastLoginTime()), formatTime(user.getCreateTime()))).toList();
    }

    /**
     * 批量加载组织名称（一次 IN 查询，避免循环查库）。
     */
    private Map<Long, String> loadDeptNames(List<SysUser> users) {
        Set<Long> deptIds = users.stream()
            .map(SysUser::getDeptId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Map.of();
        }
        return sysDeptMapper.selectBatchIds(deptIds).stream()
            .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName));
    }

    /**
     * 批量加载用户角色编码映射（user_role + role 各一次 IN 查询，避免循环查库）。
     */
    private Map<Long, List<String>> loadRoleCodesByUserIds(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
            new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, userIds));
        if (userRoles.isEmpty()) {
            return Map.of();
        }
        Set<Long> roleIds = userRoles.stream()
            .map(SysUserRole::getRoleId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> roleCodeMap = loadRoleCodeMap(roleIds);
        return userRoles.stream()
            .collect(Collectors.groupingBy(SysUserRole::getUserId,
                Collectors.mapping(ur -> roleCodeMap.get(ur.getRoleId()),
                    Collectors.filtering(Objects::nonNull, Collectors.toList()))));
    }

    /**
     * 按角色 ID 集合批量加载角色编码映射。
     */
    private Map<Long, String> loadRoleCodeMap(Collection<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return Map.of();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
            .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleCode));
    }

    /**
     * 校验用户存在，不存在抛 18005（@TableLogic 自动过滤已删除行）。
     */
    private SysUser requireUser(Long id) {
        SysUser user = sysUserMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 " + id + " 不存在");
        }
        return user;
    }

    /**
     * 校验组织存在，不存在抛 18024。
     */
    private void requireDeptExists(Long deptId) {
        if (sysDeptMapper.selectById(deptId) == null) {
            throw new BusinessException(SystemManageErrorCode.DEPT_NOT_FOUND, "组织 " + deptId + " 不存在");
        }
    }

    /**
     * 校验角色集合全部存在，否则抛 18014（一次 IN 计数，非循环查库）。
     */
    private void validateRolesExist(List<Long> roleIds) {
        if (roleIds.isEmpty()) {
            return;
        }
        Long count = sysRoleMapper.selectCount(new LambdaQueryWrapper<SysRole>().in(SysRole::getId, roleIds));
        if (count == null || count != roleIds.size()) {
            throw new BusinessException(RoleMenuErrorCode.ROLE_NOT_FOUND,
                "角色不存在，请刷新角色列表后重试");
        }
    }

    /**
     * 角色 ID 去重去空。
     */
    private List<Long> distinctRoleIds(List<Long> roleIds) {
        if (roleIds == null) {
            return List.of();
        }
        return roleIds.stream().filter(Objects::nonNull).distinct().toList();
    }

    /**
     * 写入用户-角色关联（单用户角色数个位数，逐条插入即可，无需批量基建）。
     */
    private void insertUserRoles(Long userId, List<Long> roleIds) {
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 可访问仓库 ID 集合序列化（逗号串）：null 不变、空集合存空串（显式清空语义）。
     */
    private String joinWarehouseIds(List<Long> warehouseIds) {
        if (warehouseIds == null) {
            return null;
        }
        return warehouseIds.stream().filter(Objects::nonNull).map(String::valueOf).collect(Collectors.joining(","));
    }

    /**
     * 可访问仓库 ID 集合解析（逗号串 → Long 集合）。
     */
    private List<Long> parseWarehouseIds(String warehouseIds) {
        if (warehouseIds == null || warehouseIds.isBlank()) {
            return null;
        }
        return Arrays.stream(warehouseIds.split(","))
            .map(String::trim)
            .filter(part -> !part.isEmpty())
            .map(Long::valueOf)
            .toList();
    }

    /**
     * 停用后使该用户全部会话失效：删除 access 会话与 refresh 标记（Redis 不可用时降级 WARN）。
     */
    private void revokeUserSessions(Long userId) {
        try {
            deleteByPattern(RedisKeyConstants.SESSION_PREFIX + userId + ":*");
            deleteByPattern(RedisKeyConstants.SESSION_REFRESH_PREFIX + userId + ":*");
        } catch (RedisConnectionFailureException e) {
            log.warn("停用用户后会话清理 Redis 失败，已降级处理：{}", e.getMessage());
        }
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

    /**
     * 口令复杂度：长度 ≥8 且至少包含大写、小写、数字、特殊字符中的 3 类（与 4.8 修改本人密码一致）。
     */
    private boolean matchesComplexity(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        int categories = 0;
        if (password.chars().anyMatch(Character::isUpperCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isLowerCase)) {
            categories++;
        }
        if (password.chars().anyMatch(Character::isDigit)) {
            categories++;
        }
        if (password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch))) {
            categories++;
        }
        return categories >= 3;
    }

    /**
     * 生成随机初始口令：12 位，4 类字符各至少 1 个，Fisher-Yates 打乱避免固定模式。
     */
    private String generateRandomPassword() {
        List<String> categories = List.of(PASSWORD_UPPER, PASSWORD_LOWER, PASSWORD_DIGIT, PASSWORD_SPECIAL);
        StringBuilder sb = new StringBuilder();
        for (String category : categories) {
            sb.append(category.charAt(SECURE_RANDOM.nextInt(category.length())));
        }
        while (sb.length() < RANDOM_PASSWORD_LENGTH) {
            String category = categories.get(SECURE_RANDOM.nextInt(categories.size()));
            sb.append(category.charAt(SECURE_RANDOM.nextInt(category.length())));
        }
        char[] chars = sb.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = chars[i];
            chars[i] = chars[j];
            chars[j] = tmp;
        }
        return new String(chars);
    }

    /**
     * 手机号掩码：保留前 3 后 4（S-06，与 4.5 当前用户信息一致）。
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 8) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String formatTime(LocalDateTime time) {
        return time != null ? DATETIME_FORMATTER.format(time) : null;
    }

    /**
     * CSV 字段转义：包含逗号/引号/换行时加双引号包裹并转义内部引号。
     */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\r") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private int resolvePageNum(UserQueryDTO query) {
        if (query.pageNum() == null || query.pageNum() < 1) {
            return PageConstants.DEFAULT_PAGE_NUM;
        }
        return query.pageNum();
    }

    /**
     * 每页条数上限按 PageConstants.MAX_PAGE_SIZE 截断（接口文档 1.4）。
     */
    private int resolvePageSize(UserQueryDTO query) {
        if (query.pageSize() == null || query.pageSize() < 1) {
            return PageConstants.DEFAULT_PAGE_SIZE;
        }
        return Math.min(query.pageSize(), PageConstants.MAX_PAGE_SIZE);
    }

    private boolean isAdmin(SysUser user) {
        return user.getIsAdmin() != null && user.getIsAdmin() == 1;
    }
}
