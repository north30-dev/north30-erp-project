package me.north30.erp.system.core.security;

import me.north30.erp.common.constant.RedisKeyConstants;
import me.north30.erp.common.jwt.JwtTokenProvider;
import me.north30.erp.system.core.service.UserAccessService;
import me.north30.erp.system.core.service.dto.UserSecurityData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * JWT → Authentication 转换器：校验 token 类型、Redis 会话白名单与账号状态，
 * 并实时装载角色与权限点为 GrantedAuthority（角色加 ROLE_ 前缀，权限点原样）。
 */
@Component
public class JwtLoginUserConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final StringRedisTemplate stringRedisTemplate;
    private final UserAccessService userAccessService;

    public JwtLoginUserConverter(StringRedisTemplate stringRedisTemplate,
                                 UserAccessService userAccessService) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userAccessService = userAccessService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // 1. type 必须为 access（refresh token 访问业务接口 → 401）
        String type = jwt.getClaimAsString(JwtTokenProvider.CLAIM_TYPE);
        if (!JwtTokenProvider.TYPE_ACCESS.equals(type)) {
            throw new InsufficientAuthenticationException("令牌类型不匹配，禁止访问业务接口");
        }
        Long userId;
        try {
            userId = Long.valueOf(jwt.getSubject());
        } catch (NumberFormatException e) {
            throw new InsufficientAuthenticationException("令牌身份信息无效");
        }
        String username = jwt.getClaimAsString(JwtTokenProvider.CLAIM_USERNAME);
        String jti = jwt.getId();
        // 2. Redis 会话白名单校验（登出/停用/会话过期后不存在 → 401）
        Boolean sessionExists = stringRedisTemplate.hasKey(RedisKeyConstants.sessionKey(userId, jti));
        if (!Boolean.TRUE.equals(sessionExists)) {
            throw new InsufficientAuthenticationException("登录会话不存在或已失效");
        }
        // 3. 账号状态 + 角色权限点实时查询（用户停用 → 401）
        UserSecurityData data = userAccessService.loadByUserId(userId);
        if (data == null || data.status() == null || data.status() != 1) {
            throw new DisabledException("账号已停用或不存在");
        }
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String roleCode : data.roleCodes()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }
        for (String perm : data.perms()) {
            authorities.add(new SimpleGrantedAuthority(perm));
        }
        LoginUser loginUser = new LoginUser(userId, username, jti,
            jwt.getClaimAsString(JwtTokenProvider.CLAIM_REFRESH_JTI));
        return new UsernamePasswordAuthenticationToken(loginUser, null, authorities);
    }
}
