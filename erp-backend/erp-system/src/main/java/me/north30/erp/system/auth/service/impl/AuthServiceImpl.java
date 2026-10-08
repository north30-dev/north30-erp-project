package me.north30.erp.system.auth.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.constant.TtlConstants;
import me.north30.erp.common.exception.BusinessException;
import me.north30.erp.common.exception.CommonErrorCode;
import me.north30.erp.common.exception.SystemErrorCode;
import me.north30.erp.common.jwt.IssuedToken;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.common.util.DesensitizeUtil;
import me.north30.erp.common.util.WarehouseIdCodecUtil;
import me.north30.erp.common.web.RequestContextUtil;
import me.north30.erp.system.auth.dto.ChangePasswordDTO;
import me.north30.erp.system.auth.dto.LoginDTO;
import me.north30.erp.system.auth.dto.RefreshTokenDTO;
import me.north30.erp.system.log.entity.SysLoginLog;
import me.north30.erp.system.user.entity.SysUser;
import me.north30.erp.system.common.enums.LoginTypeEnum;
import me.north30.erp.system.security.LoginUser;
import me.north30.erp.system.security.SecurityUtils;
import me.north30.erp.system.user.strategy.PasswordStrategy;
import me.north30.erp.system.auth.service.AuthService;
import me.north30.erp.system.log.service.SysLoginLogService;
import me.north30.erp.system.auth.service.UserAccessService;
import me.north30.erp.system.auth.dto.UserSecurityData;
import me.north30.erp.system.common.util.CaptchaUtil;
import me.north30.erp.system.auth.vo.CaptchaVO;
import me.north30.erp.system.auth.vo.ChangePasswordVO;
import me.north30.erp.system.auth.vo.CurrentUserVO;
import me.north30.erp.system.auth.vo.LoginVO;
import me.north30.erp.system.auth.vo.LogoutVO;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.auth.vo.RefreshTokenVO;
import me.north30.erp.system.auth.vo.UserPermsVO;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 认证授权服务实现。
 * <p>令牌失效机制（详细设计 2.5/ADR-11）：Redis 会话白名单 + 短 TTL；
 * 数据库写操作与 Redis 操作分离，事务提交后再写缓存；Redis 不可用时降级仅验签并 WARN。
 * 用户/部门/角色/菜单/权限点/数据范围取数编排统一收敛至 {@link UserAccessService}，
 * 本类只依赖该聚合服务完成认证与账号自治流程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final UserAccessService userAccessService;
    private final SysLoginLogService sysLoginLogService;

    @Override
    public CaptchaVO createCaptcha() {
        String captchaKey = UUID.randomUUID().toString().replace("-", "");
        CaptchaUtil.Captcha captcha = CaptchaUtil.create();
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.captchaKey(captchaKey), captcha.code(),
                TtlConstants.CAPTCHA_SECONDS, TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("验证码写入 Redis 失败，已降级处理：{}", e.getMessage());
        }
        return new CaptchaVO(captchaKey, captcha.dataUri(), TtlConstants.CAPTCHA_SECONDS);
    }

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 校验验证码（GETDEL 原子操作，一次有效）
        String cachedCaptcha;
        try {
            cachedCaptcha = stringRedisTemplate.opsForValue().getAndDelete(RedisKeyConstants.captchaKey(dto.captchaKey()));
        } catch (RedisConnectionFailureException e) {
            log.warn("验证码读取 Redis 失败，已降级跳过校验：{}", e.getMessage());
            cachedCaptcha = dto.captcha();
        }
        if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(dto.captcha())) {
            throw new BusinessException(SystemErrorCode.CAPTCHA_ERROR);
        }
        // 2. 账号存在性校验（登录失败统一提示 18001，避免泄露账号存在性）
        SysUser user = userAccessService.getByUsername(dto.username());
        if (user == null) {
            recordLoginLog(null, dto.username(), LoginTypeEnum.LOGIN_FAIL, false, "用户名或密码错误");
            throw new BusinessException(SystemErrorCode.USERNAME_PASSWORD_ERROR);
        }
        // 3. 停用校验
        if (user.getStatus() == null || user.getStatus() != 1) {
            recordLoginLog(user.getId(), user.getUsername(), LoginTypeEnum.LOGIN_FAIL, false, "账号已停用");
            throw new BusinessException(SystemErrorCode.ACCOUNT_DISABLED);
        }
        // 4. 锁定校验（Redis 失败计数 ≥5 次，30 分钟窗口）
        String failKey = RedisKeyConstants.loginFailKey(dto.username());
        String failCount;
        try {
            failCount = stringRedisTemplate.opsForValue().get(failKey);
        } catch (RedisConnectionFailureException e) {
            log.warn("登录失败计数读取 Redis 失败，已降级跳过锁定校验：{}", e.getMessage());
            failCount = null;
        }
        if (failCount != null && Integer.parseInt(failCount) >= TtlConstants.LOGIN_FAIL_LOCK_THRESHOLD) {
            recordLoginLog(user.getId(), user.getUsername(), LoginTypeEnum.LOGIN_FAIL, false, "账号已锁定");
            throw new BusinessException(SystemErrorCode.ACCOUNT_LOCKED,
                "账号已锁定，请 " + TtlConstants.LOGIN_FAIL_TTL.toMinutes() + " 分钟后重试");
        }
        // 5. 口令校验：失败则累加 Redis 计数（TTL 30 分钟）后记录日志并抛出
        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            try {
                Long count = stringRedisTemplate.opsForValue().increment(failKey);
                stringRedisTemplate.expire(failKey, TtlConstants.LOGIN_FAIL_TTL);
                log.info("登录失败计数 | username: {} | 次数: {}", dto.username(), count);
            } catch (RedisConnectionFailureException e) {
                log.warn("登录失败计数写入 Redis 失败，已降级处理：{}", e.getMessage());
            }
            recordLoginLog(user.getId(), user.getUsername(), LoginTypeEnum.LOGIN_FAIL, false, "用户名或密码错误");
            throw new BusinessException(SystemErrorCode.USERNAME_PASSWORD_ERROR);
        }
        // 6. 签发令牌对：先 refresh 再 access（access 携带 rjti 供登出定位失效）
        IssuedToken refresh = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername());
        IssuedToken access = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), refresh.jti());
        // 7. 写登录日志 + 更新最后登录信息（各写操作独立事务）
        recordLoginLog(user.getId(), user.getUsername(), LoginTypeEnum.LOGIN, true, null);
        LocalDateTime now = LocalDateTime.now();
        userAccessService.updateLastLogin(user.getId(), RequestContextUtil.resolveClientIp(), now);
        // 8. 事务提交后写 Redis 会话白名单（登出/停用即时失效的关键）
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.sessionKey(user.getId(), access.jti()),
                "1", access.ttlSeconds(), TimeUnit.SECONDS);
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.sessionRefreshKey(user.getId(), refresh.jti()),
                "1", refresh.ttlSeconds(), TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("会话写入 Redis 失败，降级为仅验签模式：{}", e.getMessage());
        }
        // 9. 组装返回
        boolean passwordExpired = isPasswordExpired(user.getPasswordUpdateTime());
        LoginVO.LoginUserInfoVO userInfo = new LoginVO.LoginUserInfoVO(user.getId(), user.getUsername(),
            user.getRealName(), user.getDeptId(), userAccessService.getDeptName(user.getDeptId()),
            userAccessService.listRoleCodes(user.getId()), user.getIsAdmin(), passwordExpired);
        return new LoginVO(access.token(), "Bearer", (int) access.ttlSeconds(), refresh.token(), userInfo);
    }

    @Override
    public RefreshTokenVO refreshToken(RefreshTokenDTO dto) {
        // 1. 解析并校验 refresh token（验签/有效期/iss/type=refresh）
        var jwt = parseRefreshToken(dto.refreshToken());
        Long userId = Long.valueOf(jwt.getSubject());
        String username = jwt.getClaimAsString(JwtTokenProvider.CLAIM_USERNAME);
        String jti = jwt.getId();
        // 2. jti 失效集合校验（登出后加入，不可再刷新）
        Boolean invalid;
        try {
            invalid = stringRedisTemplate.hasKey(RedisKeyConstants.refreshInvalidKey(userId, jti));
        } catch (RedisConnectionFailureException e) {
            log.warn("失效集合读取 Redis 失败，已降级处理：{}", e.getMessage());
            invalid = Boolean.FALSE;
        }
        if (Boolean.TRUE.equals(invalid)) {
            throw new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 3. refresh 会话标记校验（登出删除、口令修改删除后均不可刷新）
        Boolean markerExists;
        try {
            markerExists = stringRedisTemplate.hasKey(RedisKeyConstants.sessionRefreshKey(userId, jti));
        } catch (RedisConnectionFailureException e) {
            log.warn("会话标记读取 Redis 失败，已降级处理：{}", e.getMessage());
            markerExists = Boolean.TRUE;
        }
        if (!Boolean.TRUE.equals(markerExists)) {
            throw new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID);
        }
        // 4. 账号状态校验
        SysUser user = userAccessService.getUserById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(SystemErrorCode.ACCOUNT_DISABLED);
        }
        // 5. 签发新 access token（不延长 refresh 有效期）
        IssuedToken access = jwtTokenProvider.createAccessToken(userId, username, jti);
        recordLoginLog(userId, username, LoginTypeEnum.REFRESH, true, null);
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.sessionKey(userId, access.jti()),
                "1", access.ttlSeconds(), TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("会话重写 Redis 失败，降级为仅验签模式：{}", e.getMessage());
        }
        return new RefreshTokenVO(access.token(), "Bearer", (int) access.ttlSeconds());
    }

    @Override
    public LogoutVO logout() {
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        // 删除 access 会话与 refresh 标记，refresh jti 加入失效集合（TTL = refresh 有效期）
        try {
            stringRedisTemplate.delete(RedisKeyConstants.sessionKey(currentUser.userId(), currentUser.jti()));
            stringRedisTemplate.delete(RedisKeyConstants.sessionRefreshKey(currentUser.userId(), currentUser.refreshJti()));
            stringRedisTemplate.opsForValue().set(RedisKeyConstants.refreshInvalidKey(currentUser.userId(), currentUser.refreshJti()),
                "1", jwtTokenProvider.getProperties().getRefreshTtl().toSeconds(), TimeUnit.SECONDS);
        } catch (RedisConnectionFailureException e) {
            log.warn("登出清理 Redis 失败，已降级处理：{}", e.getMessage());
        }
        recordLoginLog(currentUser.userId(), currentUser.username(), LoginTypeEnum.LOGOUT, true, null);
        return new LogoutVO(LocalDateTime.now().format(DATETIME_FORMATTER));
    }

    @Override
    @Transactional(readOnly = true)
    public CurrentUserVO currentUser() {
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        SysUser user = userAccessService.getUserById(currentUser.userId());
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 " + currentUser.username() + " 不存在");
        }
        UserSecurityData securityData = userAccessService.loadByUserId(user.getId());
        List<String> roleCodes = securityData != null ? securityData.roleCodes() : List.of();
        Integer dataScope = securityData != null ? securityData.widestDataScope() : 6;
        return new CurrentUserVO(
            user.getId(),
            user.getUserCode(),
            user.getUsername(),
            user.getRealName(),
            user.getDeptId(),
            userAccessService.getDeptName(user.getDeptId()),
            DesensitizeUtil.maskPhone(user.getPhone()),
            user.getEmail(),
            WarehouseIdCodecUtil.parse(user.getWarehouseIds()),
            roleCodes,
            dataScope,
            user.getLastLoginTime() != null ? user.getLastLoginTime().format(DATETIME_FORMATTER) : null,
            user.getLastLoginIp()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuTreeVO> currentUserMenus() {
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        SysUser user = userAccessService.getUserById(currentUser.userId());
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 " + currentUser.username() + " 不存在");
        }
        // 菜单取数编排收敛至聚合服务：admin 全量组树，普通用户按角色并集一次查全量启用菜单内存补父链
        return userAccessService.listMenuTree(user.getId(), user.isAdmin());
    }

    @Override
    public UserPermsVO currentUserPerms() {
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        UserSecurityData data = userAccessService.loadByUserId(currentUser.userId());
        if (data == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 " + currentUser.username() + " 不存在");
        }
        return new UserPermsVO(data.perms(), data.roleCodes());
    }

    @Override
    public ChangePasswordVO changePassword(ChangePasswordDTO dto) {
        LoginUser currentUser = SecurityUtils.requireCurrentUser();
        SysUser user = userAccessService.getUserById(currentUser.userId());
        if (user == null) {
            throw new BusinessException(SystemErrorCode.USER_NOT_FOUND, "用户 " + currentUser.username() + " 不存在");
        }
        // 1. 原口令校验
        if (!passwordEncoder.matches(dto.oldPassword(), user.getPassword())) {
            throw new BusinessException(SystemErrorCode.OLD_PASSWORD_ERROR);
        }
        // 2. 新口令复杂度：≥8 位且含大小写/数字/特殊字符中至少 3 类（规则收敛于 PasswordStrategy）
        if (!PasswordStrategy.matches(dto.newPassword())) {
            throw new BusinessException(SystemErrorCode.PASSWORD_COMPLEXITY_ERROR);
        }
        // 3. 确认口令一致
        if (!dto.newPassword().equals(dto.confirmPassword())) {
            throw new BusinessException(CommonErrorCode.PARAM_ERROR, "新密码与确认密码不一致");
        }
        // 4. 更新口令与口令修改时间（写事务），变更后强制重新登录（删除全部会话）
        LocalDateTime now = LocalDateTime.now();
        user.setPassword(passwordEncoder.encode(dto.newPassword()));
        user.setPasswordUpdateTime(now);
        userAccessService.updateUser(user);
        invalidateUserSessions(user.getId());
        log.info("用户修改密码成功，已强制重新登录 | userId: {}", user.getId());
        return new ChangePasswordVO(now.format(DATETIME_FORMATTER), Boolean.TRUE);
    }

    // ------------------------------------------------------------------
    // 私有辅助方法
    // ------------------------------------------------------------------

    /**
     * 解析 refresh token：验签/type 校验由 JwtTokenProvider 承担，
     * 第三方 JwtException 统一翻译为业务码 10102（非吞异常，翻译后重新抛出）。
     */
    private Jwt parseRefreshToken(String refreshToken) {
        try {
            return jwtTokenProvider.parse(refreshToken, JwtTokenProvider.TYPE_REFRESH);
        } catch (JwtException e) {
            log.warn("refresh token 解析失败：{}", e.getMessage());
            throw new BusinessException(CommonErrorCode.REFRESH_TOKEN_INVALID);
        }
    }

    /**
     * 口令是否已过 90 天有效期。
     */
    private boolean isPasswordExpired(LocalDateTime passwordUpdateTime) {
        return passwordUpdateTime != null
            && passwordUpdateTime.plus(TtlConstants.PASSWORD_VALID_DURATION).isBefore(LocalDateTime.now());
    }

    /**
     * 记录登录日志（独立写事务，失败不影响主流程语义由调用方决定）。
     */
    private void recordLoginLog(Long userId, String username, LoginTypeEnum loginType,
                                boolean success, String failReason) {
        try {
            SysLoginLog loginLog = new SysLoginLog();
            loginLog.setUserId(userId);
            loginLog.setUsername(username);
            loginLog.setLoginType(loginType.getCode());
            loginLog.setLoginTime(LocalDateTime.now());
            loginLog.setLoginIp(RequestContextUtil.resolveClientIp());
            loginLog.setUserAgent(RequestContextUtil.resolveUserAgent());
            loginLog.setResultStatus(success ? 1 : 0);
            loginLog.setFailReason(failReason);
            sysLoginLogService.record(loginLog);
        } catch (Exception e) {
            // 日志落库失败仅告警，不阻断登录主流程（详见 SYS-01 降级约定）
            log.error("登录日志写入失败 | username: {} | type: {}", username, loginType, e);
        }
    }

    /**
     * 口令修改后使该用户全部会话失效：删除 access 会话与 refresh 标记（Redis 不可用时降级 WARN）。
     */
    private void invalidateUserSessions(Long userId) {
        try {
            deleteByPattern(RedisKeyConstants.SESSION_PREFIX + userId + ":*");
            deleteByPattern(RedisKeyConstants.SESSION_REFRESH_PREFIX + userId + ":*");
        } catch (RedisConnectionFailureException e) {
            log.warn("口令修改后会话清理 Redis 失败，已降级处理：{}", e.getMessage());
        }
    }

    private void deleteByPattern(String pattern) {
        Set<String> keys = stringRedisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
        }
    }

}
