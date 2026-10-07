package me.north30.erp.system.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import me.north30.erp.common.result.Result;
import me.north30.erp.system.auth.dto.ChangePasswordDTO;
import me.north30.erp.system.auth.dto.LoginDTO;
import me.north30.erp.system.auth.dto.RefreshTokenDTO;
import me.north30.erp.system.auth.service.AuthService;
import me.north30.erp.system.auth.vo.CaptchaVO;
import me.north30.erp.system.auth.vo.ChangePasswordVO;
import me.north30.erp.system.auth.vo.CurrentUserVO;
import me.north30.erp.system.auth.vo.LoginVO;
import me.north30.erp.system.auth.vo.LogoutVO;
import me.north30.erp.system.menu.vo.MenuTreeVO;
import me.north30.erp.system.auth.vo.RefreshTokenVO;
import me.north30.erp.system.auth.vo.UserPermsVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 认证授权接口（/api/auth，接口文档第四章 8 个接口）。
 * <p>captcha/login/refresh 免认证（SecurityConfig 白名单），其余需登录态。</p>
 */
@Tag(name = "认证授权", description = "/api/auth，接口文档 4")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** 422 业务校验失败示例（body.code 携带业务错误码，GlobalExceptionHandler 统一处理） */
    private static final String BIZ_ERROR_EXAMPLE = """
        {"code": 18001, "message": "用户名或密码错误", "data": null, "timestamp": "2026-01-01 00:00:00", "traceId": "..."}\
        """;

    private final AuthService authService;

    /**
     * 4.1 获取图形验证码（免认证）。
     */
    @PostMapping("/captcha")
    @Operation(summary = "获取图形验证码", description = "匿名接口（白名单），验证码有效期见返回值 expiresSeconds")
    public Result<CaptchaVO> captcha() {
        return Result.success(authService.createCaptcha());
    }

    /**
     * 4.2 用户登录（免认证）。
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "匿名接口（白名单）；连续失败 5 次锁定 30 分钟，返回双令牌与用户信息")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18004 验证码错误或已过期/18001 用户名或密码错误/18003 账号已停用/18002 账号已锁定）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /**
     * 4.3 刷新令牌（免认证，须携带有效 refresh token）。
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "匿名接口（白名单），须携带有效 refresh token；仅签发新 access token，不延长 refresh 有效期")
    @ApiResponse(responseCode = "422", description = "业务校验失败（10102 刷新令牌无效或已失效/18003 账号已停用）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(authService.refreshToken(dto));
    }

    /**
     * 4.4 用户登出（登录态即可）。
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "登录态接口；access 会话与 refresh 标记一并失效，refresh jti 加入失效集合")
    public Result<LogoutVO> logout() {
        return Result.success(authService.logout());
    }

    /**
     * 4.5 获取当前用户信息。
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "登录态接口；手机号脱敏返回（保留前 3 后 4）")
    public Result<CurrentUserVO> me() {
        return Result.success(authService.currentUser());
    }

    /**
     * 4.6 获取当前用户菜单树。
     */
    @GetMapping("/menus")
    @Operation(summary = "获取当前用户菜单树", description = "登录态接口；admin 返回全量菜单树，普通用户按角色菜单并集组树")
    public Result<List<MenuTreeVO>> menus() {
        return Result.success(authService.currentUserMenus());
    }

    /**
     * 4.7 获取当前用户权限点集合。
     */
    @GetMapping("/perms")
    @Operation(summary = "获取当前用户权限点集合", description = "登录态接口；返回权限点与角色编码集合，供前端按钮级控制")
    public Result<UserPermsVO> perms() {
        return Result.success(authService.currentUserPerms());
    }

    /**
     * 4.8 修改本人密码。
     */
    @PutMapping("/password")
    @Operation(summary = "修改本人密码", description = "登录态接口；修改成功后全部在线会话失效，须重新登录")
    @ApiResponse(responseCode = "422", description = "业务校验失败（18008 原密码错误/18009 新密码复杂度不足/10001 两次输入的新密码不一致/18005 用户不存在）",
        content = @Content(mediaType = "application/json",
            examples = @ExampleObject(value = BIZ_ERROR_EXAMPLE)))
    public Result<ChangePasswordVO> password(@Valid @RequestBody ChangePasswordDTO dto) {
        return Result.success(authService.changePassword(dto));
    }
}
